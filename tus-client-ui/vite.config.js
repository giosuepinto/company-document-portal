import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        strictPort: true,
        // Il proxy inoltra le richieste al Gateway (8080)
        // facendo credere al browser di essere sulla stessa origine.
        proxy: {
            '/api': 'http://localhost:8080',
            '/login': 'http://localhost:8080',
            '/logout': 'http://localhost:8080',
            '/oauth2': 'http://localhost:8080',
            '/uploads': 'http://localhost:8080'
        }
    }
})