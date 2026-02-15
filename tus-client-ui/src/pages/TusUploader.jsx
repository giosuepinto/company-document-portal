import React, { useState } from 'react';
import { Card, Button, ProgressBar, Alert, Spinner, Row, Col, Form } from 'react-bootstrap';
// --- FIX IMPORT ---
import { Upload } from "tus-js-client";
import { API_BASE_URL } from '../utils/config';
import { getCookie } from '../utils/utils'; // Importa helper

// Usa percorso relativo per sfruttare il proxy di Vite (http://localhost:5173/uploads -> http://localhost:8080/uploads)
const TUS_ENDPOINT = "/uploads";

function TusUploader({
                         file, progress, isUploading, uploadUrl, error, chunkSize, setChunkSize,
                         uploadMode, setUploadMode, handleFileChange, handleCancelUpload,
                         setError, fileIdToUpdate, setFileIdToUpdate, uploadRef, setIsUploading, setProgress, setUploadUrl, setFile
                     }) {
    const [connectionStatus, setConnectionStatus] = useState('idle');
    const [serverInfo, setServerInfo] = useState(null);

    // Test connessione (OPTIONS)
    const handleConnectionTest = async () => {
        setConnectionStatus('pending');
        try {
            // Fetch è ok qui se non servono credenziali, ma meglio uniformare
            const response = await fetch(TUS_ENDPOINT, { method: 'OPTIONS' });
            if (!response.ok) throw new Error(`Status ${response.status}`);

            setServerInfo({
                version: response.headers.get('Tus-Version'),
                maxSize: response.headers.get('Tus-Max-Size'),
            });
            setConnectionStatus('success');
        } catch (err) {
            setConnectionStatus('error');
            setError("Connection failed: " + err.message);
        }
    };

    const handleUpload = () => {
        if (!file) {
            setError("Please select a file first.");
            return;
        }

        setIsUploading(true);
        setError("");

        if (uploadRef.current) {
            console.log("Resuming existing upload...");
            uploadRef.current.start();
            return;
        }

        setUploadUrl(null);
        setProgress(0);

        // --- FIX URL E CSRF ---
        const options = {
            // Se aggiornamento versione: /api/v1/files/{id}/versions
            // Se nuovo upload: /uploads (tramite proxy)
            endpoint: fileIdToUpdate
                ? `${API_BASE_URL}/files/${fileIdToUpdate}/versions`
                : TUS_ENDPOINT,

            retryDelays: [0, 3000, 5000],
            chunkSize: chunkSize,
            metadata: {
                filename: file.name,
                contentType: file.type
            },
            headers: {
                // TUS richiede l'inserimento manuale del CSRF
                "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
            },
            onBeforeRequest: function (req) {
                const xhr = req.getUnderlyingObject();
                // Assicura l'invio dei cookie
                xhr.withCredentials = true;
            },
            onError: (error) => {
                console.error("Upload Error:", error);
                let message = error.message;
                if (error.originalResponse) {
                    message += ` (Status: ${error.originalResponse.getStatus()})`;
                }
                setError(`Upload failed: ${message}`);
                setIsUploading(false);
                uploadRef.current = null;
            },
            onProgress: (bytesUploaded, bytesTotal) => {
                const percentage = (bytesUploaded / bytesTotal) * 100;
                setProgress(percentage);
            },
            onSuccess: () => {
                console.log("Upload complete!");
                if (uploadRef.current) {
                    setUploadUrl(uploadRef.current.url);
                }
                setFile(null);
                setFileIdToUpdate(null);
                setIsUploading(false);
                uploadRef.current = null;
            },
            onChunkComplete: (chunkSize, bytesAccepted, bytesTotal) => {
                if (uploadMode === 'step' && bytesAccepted < bytesTotal) {
                    console.log("Chunk uploaded. Pausing for Step-by-Step.");
                    if (uploadRef.current) {
                        uploadRef.current.abort(false);
                        setIsUploading(false);
                    }
                }
            }
        };

        const upload = new Upload(file, options);
        uploadRef.current = upload;
        upload.start();
    };

    const isUploadInProgress = (progress > 0 && progress < 100) || (isUploading && progress === 0);
    const buttonText = isUploading
        ? "Uploading..."
        : (isUploadInProgress && uploadMode === 'step' ? "Upload Next Chunk" : "Start Upload");

    return (
        <Card className="mx-auto" style={{ maxWidth: '600px', marginTop: '20px' }}>
            <Card.Header as="h4" className="text-center">
                {fileIdToUpdate ? "Update Version" : "New Upload (TUS)"}
            </Card.Header>
            <Card.Body>
                {fileIdToUpdate && (
                    <Alert variant="warning" className="d-flex justify-content-between align-items-center">
                        <span>Uploading new version...</span>
                        <Button variant="outline-dark" size="sm" onClick={() => setFileIdToUpdate(null)}>Cancel</Button>
                    </Alert>
                )}

                <div className="text-center mb-4 border-bottom pb-4">
                    <Button variant="outline-secondary" onClick={handleConnectionTest} disabled={connectionStatus === 'pending'}>
                        {connectionStatus === 'pending' ? <Spinner size="sm" animation="border" /> : "Test Server"}
                    </Button>
                    {connectionStatus === 'success' && serverInfo && (
                        <Alert variant="success" className="mt-2 text-start">
                            <small>TUS v{serverInfo.version} | Max: {serverInfo.maxSize || 'Unlimited'}</small>
                        </Alert>
                    )}
                </div>

                <Row className="mb-3">
                    <Col md={6}>
                        <Form.Group>
                            <Form.Label>Upload Mode</Form.Label>
                            <div>
                                <Form.Check inline type="radio" label="Full" name="uploadMode" id="full-upload"
                                            checked={uploadMode === 'full'} onChange={() => setUploadMode('full')} disabled={isUploading}
                                />
                                <Form.Check inline type="radio" label="Step-by-Step" name="uploadMode" id="step-upload"
                                            checked={uploadMode === 'step'} onChange={() => setUploadMode('step')} disabled={isUploading}
                                />
                            </div>
                        </Form.Group>
                    </Col>
                    <Col md={6}>
                        <Form.Group controlId="chunkSize">
                            <Form.Label>Chunk Size (bytes)</Form.Label>
                            <Form.Control type="number" step="1024" value={chunkSize}
                                          onChange={(e) => setChunkSize(parseInt(e.target.value, 10))} disabled={isUploading}
                            />
                        </Form.Group>
                    </Col>
                </Row>

                <Row className="mb-3">
                    <Col>
                        <Form.Group controlId="formFile">
                            <Form.Label>Select File</Form.Label>
                            <Form.Control type="file" onChange={handleFileChange} disabled={isUploading} />
                        </Form.Group>
                    </Col>
                </Row>

                {file && (
                    <div className="mt-4 text-center">
                        <ProgressBar animated now={progress} label={`${progress.toFixed(0)}%`} className="mb-3" />
                        <Button variant="primary" onClick={handleUpload} disabled={isUploading && uploadMode === 'full'}>
                            {buttonText}
                        </Button>
                        {(isUploading || isUploadInProgress) && (
                            <Button variant="danger" className="ms-2" onClick={handleCancelUpload}>Cancel</Button>
                        )}
                    </div>
                )}

                {error && <Alert variant="danger" className="mt-3">{error}</Alert>}
                {uploadUrl && <Alert variant="success" className="mt-3">Upload Complete!</Alert>}
            </Card.Body>
        </Card>
    );
}

export default TusUploader;