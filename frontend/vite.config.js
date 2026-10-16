import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "localhost",
    port: 5173,
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          charts: ["recharts"],
          framework: ["react", "react-dom", "react-router", "react-router-dom"],
        },
      },
    },
  },
});
