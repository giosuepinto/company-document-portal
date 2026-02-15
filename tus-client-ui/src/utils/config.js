// src/utils/config.js
export const GATEWAY_URL = 'http://localhost:8080';
export const API_BASE_URL = `${GATEWAY_URL}/api/v1`;

// Rotte per il login/logout gestite da Spring Cloud Gateway (OAuth2 Client)
export const AUTH_LOGIN_URL = `${GATEWAY_URL}/oauth2/authorization/gateway-client`;
export const AUTH_LOGOUT_URL = `${GATEWAY_URL}/logout`; // Triggera RP-initiated logout