import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";
import path from "path";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      "/api/v1/ai": {
        target: "http://localhost:8084",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/processing": {
        target: "http://localhost:8084",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/users": {
        target: "http://localhost:8081",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/departments": {
        target: "http://localhost:8081",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/documents": {
        target: "http://localhost:8082",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/files": {
        target: "http://localhost:8083",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/upload": {
        target: "http://localhost:8083",
        changeOrigin: true,
        secure: false,
      },
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
