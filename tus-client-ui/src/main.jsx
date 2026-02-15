import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import 'bootstrap/dist/css/bootstrap.min.css';
import axios from 'axios';

// --- CONFIGURAZIONE GLOBALE AXIOS ---
// Invia sempre i cookie (JSESSIONID, XSRF-TOKEN)
axios.defaults.withCredentials = true;
// Legge il token dal cookie e lo imposta nell'header
axios.defaults.xsrfCookieName = "XSRF-TOKEN";
axios.defaults.xsrfHeaderName = "X-XSRF-TOKEN";
// ------------------------------------

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </StrictMode>,
)