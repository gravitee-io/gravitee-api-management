import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.test.{ts,tsx}", "src/**/__tests__/**/*.{ts,tsx}"],
    coverage: {
      provider: "v8",
      reporter: ["text", "html"],
      exclude: ["node_modules/", ".next/", "coverage/"],
    },
  },
});
