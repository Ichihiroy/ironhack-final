declare global {
  interface Window {
    __ENV?: { VITE_API_URL?: string };
  }
}

/** API base URL, resolved at runtime from env.js — empty string means same-origin. */
export const API_URL = window.__ENV?.VITE_API_URL ?? "";
