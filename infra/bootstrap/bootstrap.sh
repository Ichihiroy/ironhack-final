#!/usr/bin/env bash
# ============================================================================
# Phase 0 bootstrap — run ONCE, by a human with Owner rights, BEFORE any CI.
#
# Solves the chicken-and-egg problem: the Terraform state backend and the CI
# OIDC identities cannot be created by CI, because CI needs both to exist
# before it can authenticate or plan anything.
#
# Creates:
#   1. tfstate resource group + storage account + blob container
#   2. The application resource group (RBAC scope anchor — see docs/adr/0006)
#   3. Three Entra app registrations, federated to GitHub Actions (OIDC, no
#      secrets anywhere):
#        app-ci       → AcrPush on the app RG                      (image push + cosign sign)
#        platform-ro  → Reader on the app RG                       (terraform plan on PRs + nightly drift check)
#        platform-rw  → Contributor + RBAC Administrator on the app RG (terraform apply on main)
#
#      Deploys are pull-based (Argo CD, ADR-0008): CI never talks to AKS, so
#      app-ci needs no cluster role at all.
#
# Usage:
#   SUBSCRIPTION_ID=... GITHUB_REPO=org/repo ./bootstrap.sh
# Optional env: LOCATION (westeurope), TEAM (thelocals), PROJECT (ironhack),
#               ENVIRONMENT (prod)
# ============================================================================
set -euo pipefail

: "${SUBSCRIPTION_ID:?Set SUBSCRIPTION_ID}"
: "${GITHUB_REPO:?Set GITHUB_REPO as <org>/<repo>}"
LOCATION="${LOCATION:-westeurope}"
# Team slug prefixes every name — several teams share the subscription/region,
# and ACR/Key Vault/SQL/storage names are globally unique. MUST match the
# `team` Terraform variable.
TEAM="${TEAM:-thelocals}"
PROJECT="${PROJECT:-ironhack}"
ENVIRONMENT="${ENVIRONMENT:-prod}"

PREFIX="${TEAM}-${PROJECT}-${ENVIRONMENT}"
APP_RG="rg-${PREFIX}"
TFSTATE_RG="rg-${TEAM}-${PROJECT}-tfstate"
# storage account: lowercase alphanumeric, globally unique, <=24 chars
TFSTATE_SA="st$(echo "${TEAM}${PROJECT}tf" | tr -cd 'a-z0-9' | cut -c1-20)"
TFSTATE_CONTAINER="tfstate"
ISSUER="https://token.actions.githubusercontent.com"
AUDIENCE="api://AzureADTokenExchange"

az account set --subscription "$SUBSCRIPTION_ID"
TENANT_ID=$(az account show --query tenantId -o tsv)

echo "==> [1/4] Terraform state backend"
az group create --name "$TFSTATE_RG" --location "$LOCATION" -o none
az storage account create \
  --name "$TFSTATE_SA" \
  --resource-group "$TFSTATE_RG" \
  --location "$LOCATION" \
  --sku Standard_LRS \
  --kind StorageV2 \
  --min-tls-version TLS1_2 \
  --allow-blob-public-access false \
  -o none
az storage container create \
  --name "$TFSTATE_CONTAINER" \
  --account-name "$TFSTATE_SA" \
  --auth-mode login \
  -o none 2>/dev/null || true

echo "==> [2/4] Application resource group (RBAC scope anchor)"
az group create --name "$APP_RG" --location "$LOCATION" -o none

APP_RG_ID="/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${APP_RG}"
TFSTATE_SA_ID="/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${TFSTATE_RG}/providers/Microsoft.Storage/storageAccounts/${TFSTATE_SA}"

# ── helper: idempotent app registration + service principal + fed creds ──
create_identity() {
  local name="$1"
  local app_id
  app_id=$(az ad app list --display-name "$name" --query "[0].appId" -o tsv)
  if [[ -z "$app_id" ]]; then
    app_id=$(az ad app create --display-name "$name" --query appId -o tsv)
    az ad sp create --id "$app_id" -o none
  fi
  echo "$app_id"
}

add_fed_cred() {
  local app_id="$1" cred_name="$2" subject="$3"
  az ad app federated-credential create --id "$app_id" --parameters "{
    \"name\": \"${cred_name}\",
    \"issuer\": \"${ISSUER}\",
    \"subject\": \"${subject}\",
    \"audiences\": [\"${AUDIENCE}\"]
  }" -o none 2>/dev/null || echo "    (federated credential ${cred_name} already exists)"
}

assign_role() {
  local app_id="$1" role="$2" scope="$3"
  local sp_object_id
  sp_object_id=$(az ad sp show --id "$app_id" --query id -o tsv)
  az role assignment create --assignee-object-id "$sp_object_id" \
    --assignee-principal-type ServicePrincipal \
    --role "$role" --scope "$scope" -o none 2>/dev/null || true
}

echo "==> [3/4] CI identities + GitHub OIDC federation (repo: ${GITHUB_REPO})"

echo "  -> platform-ro (terraform plan on pull requests + scheduled drift detection)"
RO_APP_ID=$(create_identity "${PREFIX}-gh-platform-ro")
add_fed_cred "$RO_APP_ID" "pull-request" "repo:${GITHUB_REPO}:pull_request"
# scheduled runs (infra-drift.yml) present the branch subject, not pull_request
add_fed_cred "$RO_APP_ID" "branch-main" "repo:${GITHUB_REPO}:ref:refs/heads/main"
assign_role "$RO_APP_ID" "Reader" "$APP_RG_ID"
# plan reads (and locks) remote state
assign_role "$RO_APP_ID" "Storage Blob Data Contributor" "$TFSTATE_SA_ID"

echo "  -> platform-rw (terraform apply on main, gated by 'production' environment)"
RW_APP_ID=$(create_identity "${PREFIX}-gh-platform-rw")
add_fed_cred "$RW_APP_ID" "env-production" "repo:${GITHUB_REPO}:environment:production"
assign_role "$RW_APP_ID" "Contributor" "$APP_RG_ID"
# Contributor cannot create role assignments; Terraform assigns AcrPull/KV roles.
# 'Role Based Access Control Administrator' is the least-privilege alternative to Owner.
assign_role "$RW_APP_ID" "Role Based Access Control Administrator" "$APP_RG_ID"
assign_role "$RW_APP_ID" "Storage Blob Data Contributor" "$TFSTATE_SA_ID"

echo "  -> app-ci (image push + cosign sign from the app pipelines)"
APPCI_APP_ID=$(create_identity "${PREFIX}-gh-app-ci")
add_fed_cred "$APPCI_APP_ID" "branch-main" "repo:${GITHUB_REPO}:ref:refs/heads/main"
# AcrPush only: deploys are pull-based via Argo CD (ADR-0008), so this identity
# never needs AKS access — the promote jobs just commit a tag bump to git.
assign_role "$APPCI_APP_ID" "AcrPush" "$APP_RG_ID"

echo "==> [4/4] Done. Configure these as GitHub repository VARIABLES (Settings → Secrets and variables → Actions → Variables):"
cat <<EOF

  AZURE_TENANT_ID                 ${TENANT_ID}
  AZURE_SUBSCRIPTION_ID           ${SUBSCRIPTION_ID}
  AZURE_CLIENT_ID_PLATFORM_RO     ${RO_APP_ID}
  AZURE_CLIENT_ID_PLATFORM_RW     ${RW_APP_ID}
  AZURE_CLIENT_ID_APP             ${APPCI_APP_ID}
  TFSTATE_RESOURCE_GROUP          ${TFSTATE_RG}
  TFSTATE_STORAGE_ACCOUNT         ${TFSTATE_SA}
  TFSTATE_CONTAINER               ${TFSTATE_CONTAINER}
  APP_RESOURCE_GROUP              ${APP_RG}

After the first successful infra-apply, also set (from terraform outputs):

  ACR_LOGIN_SERVER                <terraform output acr_login_server>

(That is the only output CI still needs: deploys are pull-based via Argo CD,
and Terraform injects cluster/Key Vault identifiers directly into the Argo CD
Application specs — see ADR-0008.)

These are identifiers, not credentials — OIDC federation means no secret ever
leaves Azure. Remember to create the 'staging' and 'production' GitHub
environments, and add a required reviewer on 'production'.
EOF
