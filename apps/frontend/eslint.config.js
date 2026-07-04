import eslint from "@eslint/js";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["dist/", "public/env.js"] },
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
);
