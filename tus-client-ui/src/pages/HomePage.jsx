// File: pages/HomePage.jsx

import React from 'react';
import Card from 'react-bootstrap/Card';

function HomePage() {
    return (
        <Card className="mx-auto" style={{ maxWidth: '600px', marginTop: '20px' }}>
            <Card.Header as="h4" className="text-center">Welcome</Card.Header>
            <Card.Body>
                <Card.Title>File Management Service</Card.Title>
                <Card.Text>
                    Welcome to the test client for Lab 4.
                    <br /><br />
                    Use the navigation bar at the top to:
                </Card.Text>
                <ul className="mb-3">
                    <li><strong>Upload:</strong> Upload new files using the TUS protocol.</li>
                    <li><strong>Browse Files:</strong> View, download, and delete previously uploaded files.</li>
                </ul>
            </Card.Body>
        </Card>
    );
}

export default HomePage;