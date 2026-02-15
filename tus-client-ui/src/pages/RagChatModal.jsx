import React, { useState } from 'react';
import { Modal, Button, Form, Spinner, Alert, Badge } from 'react-bootstrap';
import { ChatDots, Send, FileEarmarkText } from 'react-bootstrap-icons';
import axios from 'axios';
import { API_BASE_URL } from '../utils/config';

const RagChatModal = ({ show, onHide, files = [] }) => {
    const [question, setQuestion] = useState('');
    const [answer, setAnswer] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    // NEW STATE: List of selected IDs
    const [selectedFileIds, setSelectedFileIds] = useState([]);

    // Checkbox Management: Add/Remove ID
    const handleFileToggle = (fileId) => {
        if (selectedFileIds.includes(fileId)) {
            setSelectedFileIds(selectedFileIds.filter(id => id !== fileId));
        } else {
            setSelectedFileIds([...selectedFileIds, fileId]);
        }
    };

    const handleAsk = async (e) => {
        e.preventDefault();
        if (!question.trim()) return;

        setIsLoading(true);
        setAnswer('');
        setError('');

        try {
            // Build the payload
            const payload = {
                question: question,
                // If the list is empty, send null (backend searches everywhere),
                // otherwise send the specific list
                specificFileIds: selectedFileIds.length > 0 ? selectedFileIds : null
            };

            const response = await axios.post(`${API_BASE_URL}/files/query`, payload);
            const result = response.data;

            // Response handling logic
            if (typeof result === 'string') {
                setAnswer(result);
            } else if (typeof result === 'object' && result !== null) {
                setAnswer(result.answer || JSON.stringify(result));
            } else {
                setAnswer(String(result));
            }

        } catch (err) {
            console.error("RAG Error:", err);
            setError("Unable to get an answer from the AI at the moment.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Modal show={show} onHide={onHide} size="lg" centered>
            <Modal.Header closeButton className="bg-light">
                <Modal.Title className="d-flex align-items-center text-primary">
                    <ChatDots className="me-2" />
                    Chat Assistant
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <div className="mb-3 text-muted small">
                    The AI will answer based on the documents. Select specific files or leave empty to search all.
                </div>

                {/* --- NEW SECTION: FILE SELECTION --- */}
                <div className="mb-4 border rounded p-3 bg-light">
                    <h6 className="d-flex justify-content-between align-items-center">
                        <span>🔍 Search Context</span>
                        <Badge bg="secondary">
                            {selectedFileIds.length === 0 ? "All files" : `${selectedFileIds.length} selected`}
                        </Badge>
                    </h6>

                    <div style={{ maxHeight: '150px', overflowY: 'auto', border: '1px solid #dee2e6', background: 'white', borderRadius: '4px', padding: '10px' }}>
                        {files && files.length > 0 ? (
                            files.map(file => (
                                <Form.Check
                                    key={file.id}
                                    type="checkbox"
                                    id={`file-check-${file.id}`}
                                    label={
                                        <span className="d-flex align-items-center">
                                            <FileEarmarkText className="me-2 text-muted" />
                                            <span className="text-truncate" style={{maxWidth: '300px'}}>{file.filename}</span>
                                        </span>
                                    }
                                    checked={selectedFileIds.includes(file.id)}
                                    onChange={() => handleFileToggle(file.id)}
                                    className="mb-2"
                                />
                            ))
                        ) : (
                            <div className="text-muted fst-italic small">No files available in the current view.</div>
                        )}
                    </div>
                    {selectedFileIds.length > 0 && (
                        <div className="text-end mt-1">
                            <Button variant="link" size="sm" className="p-0 text-decoration-none" onClick={() => setSelectedFileIds([])}>
                                Deselect all
                            </Button>
                        </div>
                    )}
                </div>
                {/* -------------------------------------- */}

                <Form onSubmit={handleAsk}>
                    <Form.Group className="d-flex gap-2">
                        <Form.Control
                            type="text"
                            placeholder="Ex: What does the document say about the budget?"
                            value={question}
                            onChange={(e) => setQuestion(e.target.value)}
                            disabled={isLoading}
                            autoFocus
                        />
                        <Button type="submit" variant="primary" disabled={isLoading || !question.trim()}>
                            {isLoading ? <Spinner size="sm" animation="border" /> : <Send />}
                        </Button>
                    </Form.Group>
                </Form>

                {/* Response Area */}
                <hr />
                <div className="mt-3 p-3 rounded bg-light border" style={{ minHeight: '150px' }}>
                    {isLoading && (
                        <div className="text-center text-muted py-4">
                            <Spinner animation="grow" size="sm" className="me-2" />
                            Analyzing...
                        </div>
                    )}

                    {error && <Alert variant="danger">{error}</Alert>}

                    {answer && (
                        <div>
                            <h6 className="fw-bold text-success">Answer:</h6>
                            <p style={{ whiteSpace: 'pre-wrap' }}>{answer}</p>
                        </div>
                    )}

                    {!isLoading && !answer && !error && (
                        <div className="text-center text-muted fst-italic py-4 opacity-50">
                            The answer will appear here.
                        </div>
                    )}
                </div>
            </Modal.Body>
        </Modal>
    );
};

export default RagChatModal;