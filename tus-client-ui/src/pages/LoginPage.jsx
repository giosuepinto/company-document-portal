import React from 'react';
import { Card, Button } from 'react-bootstrap';
import { AUTH_LOGIN_URL } from '../utils/config';

function LoginPage() {
    const handleLogin = () => {
        // Reindirizza l'intero browser a Keycloak tramite il Gateway
        window.location.href = AUTH_LOGIN_URL;
    };

    return (
        <Card className="mx-auto text-center" style={{ maxWidth: '400px', marginTop: '50px' }}>
            <Card.Body>
                <Card.Title>Welcome</Card.Title>
                <Card.Text>
                    To access the File Management System you need to be authenticated.
                </Card.Text>
                <Button variant="primary" onClick={handleLogin}>
                    Login with Keycloak
                </Button>
            </Card.Body>
        </Card>
    );
}

export default LoginPage;