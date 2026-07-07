package com.ironhack.backend.overcast.domain;

/** Coarse resource classification the rule predicates dispatch on. */
public enum ResourceKind {
    VM, DISK, SNAPSHOT, PUBLIC_IP, OTHER;

    public static ResourceKind fromAzureType(String resourceType) {
        if (resourceType == null) return OTHER;
        String t = resourceType.toLowerCase();
        if (t.endsWith("/virtualmachines")) return VM;
        if (t.endsWith("/disks")) return DISK;
        if (t.endsWith("/snapshots")) return SNAPSHOT;
        if (t.endsWith("/publicipaddresses")) return PUBLIC_IP;
        return OTHER;
    }
}
