import React, { useState, useEffect } from 'react';
import { Badge, Card, Table, Alert, Spinner, Button, Pagination, Modal, InputGroup, Form } from 'react-bootstrap';
import {
    Download, Upload, Trash, Share, ChevronDown, ChevronRight, FileEarmark,
    PencilSquare, ArrowCounterclockwise, InfoCircle, ShieldLock, ShieldCheck, ChatDots, ExclamationCircle
} from 'react-bootstrap-icons';
import axios from 'axios';
import { API_BASE_URL } from '../utils/config';
import { formatBytes, formatDate } from '../utils/utils.js';
import RagChatModal from '../pages/RagChatModal';

// --- HELPER: ICON OR PREVIEW ---
const FileIconOrPreview = ({ file }) => {
    const [imgError, setImgError] = useState(false);
    const previewUrl = `${API_BASE_URL}/files/${file.id}/preview`;

    if (imgError) {
        return <FileEarmark className="me-2 text-primary" size={24} />;
    }
    return (
        <img
            src={previewUrl}
            alt="preview"
            className="me-2 border rounded"
            style={{ width: '30px', height: '30px', objectFit: 'cover' }}
            onError={() => setImgError(true)}
        />
    );
};

// --- HELPER: FULL PREVIEW MODAL ---
const FilePreviewModal = ({ show, onHide, file }) => {
    const [textContent, setTextContent] = useState(null);
    const [pdfBlobUrl, setPdfBlobUrl] = useState(null);
    const [loading, setLoading] = useState(false);

    const fileUrl = file ? `${API_BASE_URL}/files/${file.id}/download?action=view` : '';

    useEffect(() => {
        if (!show || !file) {
            setTextContent(null);
            setPdfBlobUrl(null);
            return;
        }

        if (file.contentType.startsWith('text/')) {
            setLoading(true);
            axios.get(fileUrl, { responseType: 'text' })
                .then(res => setTextContent(res.data.slice(0, 2000) + (res.data.length > 2000 ? '\n... (truncated)' : '')))
                .catch(() => setTextContent("Error loading text preview"))
                .finally(() => setLoading(false));

        } else if (file.contentType === 'application/pdf') {
            setLoading(true);
            axios.get(fileUrl, { responseType: 'blob' })
                .then(res => {
                    const url = URL.createObjectURL(res.data);
                    setPdfBlobUrl(url);
                })
                .catch((err) => {
                    console.error("Error loading PDF preview", err);
                    setPdfBlobUrl(null);
                })
                .finally(() => setLoading(false));
        } else {
            setTextContent(null);
        }

        return () => {
            if (pdfBlobUrl) {
                URL.revokeObjectURL(pdfBlobUrl);
            }
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [show, file, fileUrl]);

    if (!file) return null;

    const isImage = file.contentType.startsWith('image/');
    const isPdf = file.contentType === 'application/pdf';
    const isText = file.contentType.startsWith('text/');

    return (
        <Modal show={show} onHide={onHide} size="lg" centered>
            <Modal.Header closeButton>
                <Modal.Title>Preview: {file.filename}</Modal.Title>
            </Modal.Header>
            <Modal.Body className="text-center bg-light" style={{ minHeight: '400px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                {loading && <Spinner animation="border" />}

                {!loading && isImage && (
                    <img src={fileUrl} alt="Preview" style={{ maxWidth: '100%', maxHeight: '70vh' }} />
                )}

                {!loading && isPdf && pdfBlobUrl && (
                    <iframe src={pdfBlobUrl + "#toolbar=0"} title="PDF" width="100%" height="500px" style={{ border: 'none' }} />
                )}

                {!loading && isPdf && !pdfBlobUrl && (
                    <div className="text-center">
                        <p className="text-danger">Failed to load PDF preview.</p>
                        <Button variant="primary" href={fileUrl} target="_blank">Download to View</Button>
                    </div>
                )}

                {!loading && isText && (
                    <pre className="text-start p-3 border bg-white w-100" style={{ maxHeight: '500px', overflow: 'auto' }}>
                        {textContent || "Loading..."}
                    </pre>
                )}

                {!loading && !isImage && !isPdf && !isText && (
                    <div className="text-center">
                        <p>Preview not available for this file type.</p>
                        <Button variant="primary" href={fileUrl} target="_blank">Download to View</Button>
                    </div>
                )}
            </Modal.Body>
        </Modal>
    );
};

// --- TRANSLATED AI METADATA MODAL (WITH RETRY) ---
const AiMetadataModal = ({ show, onHide, file, onRetryAi }) => {
    const [isRetrying, setIsRetrying] = useState(false);

    if (!file) return null;

    const isConfidential = file.sensitivity === 'CONFIDENTIAL';
    const sensitivityVariant = isConfidential ? 'danger' : 'success';
    const SensitivityIcon = isConfidential ? ShieldLock : ShieldCheck;

    const handleRetryClick = async () => {
        setIsRetrying(true);
        await onRetryAi(file.id);
        setIsRetrying(false);
    };

    // Fallback: se il backend passa 'aiStatus' o 'ai_status' lo usiamo. Altrimenti deduciamo.
    const isFailed = file.aiStatus === 'FAILED' || file.ai_status === 'FAILED';

    return (
        <Modal show={show} onHide={onHide} size="lg" centered>
            <Modal.Header closeButton>
                <Modal.Title className="d-flex align-items-center">
                    <InfoCircle className="me-2" />
                    Details & AI Analysis
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <div className="mb-4 p-3 bg-light rounded border">
                    <h5 className="text-primary mb-1">{file.filename}</h5>
                    <small className="text-muted">Current Version</small>
                </div>

                <div className="mb-4">
                    <h6>Security Level</h6>
                    {file.sensitivity ? (
                        <Badge bg={sensitivityVariant} className="p-2 fs-6 d-inline-flex align-items-center">
                            <SensitivityIcon className="me-2" />
                            {file.sensitivity}
                        </Badge>
                    ) : (
                        <span className="text-muted fst-italic">Waiting for classification...</span>
                    )}
                </div>

                <div className="mb-4">
                    <h6>📝 AI Summary</h6>
                    <div className="p-3 border rounded bg-white">
                        {file.summary ? (
                            <p className="mb-0" style={{ whiteSpace: 'pre-line' }}>
                                {file.summary}
                            </p>
                        ) : isFailed ? (
                            <div className="text-center text-danger py-2">
                                <ExclamationCircle className="me-2" size={24} />
                                <strong>AI Analysis Failed.</strong> Please retry.
                            </div>
                        ) : (
                            <div className="text-center text-muted py-2">
                                <Spinner animation="border" size="sm" className="me-2" />
                                AI is analyzing the document... (or stuck)
                            </div>
                        )}
                    </div>
                    {/* --- PULSANTE RETRY/RIGENERA --- */}
                    <div className="text-end mt-2">
                        <Button
                            variant={file.summary ? "outline-primary" : "warning"}
                            size="sm"
                            onClick={handleRetryClick}
                            disabled={isRetrying}
                        >
                            {isRetrying ? (
                                <><Spinner size="sm" animation="border" className="me-1"/> Processing...</>
                            ) : (
                                file.summary ? "Regenerate Analysis" : "Retry Analysis"
                            )}
                        </Button>
                    </div>
                </div>

                <div>
                    <h6>🏷️ Generated Tags</h6>
                    {file.tags && file.tags.length > 0 ? (
                        <div className="d-flex flex-wrap gap-2">
                            {file.tags.map((tag, index) => (
                                <Badge key={index} bg="info" text="dark" className="fs-6 fw-normal px-3 py-2 rounded-pill">
                                    #{tag}
                                </Badge>
                            ))}
                        </div>
                    ) : (
                        <span className="text-muted fst-italic">No tags available.</span>
                    )}
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={onHide}>Close</Button>
            </Modal.Footer>
        </Modal>
    );
};

// --- MAIN COMPONENT ---
function FileBrowser({ onStartVersionUpdate, currentUser }) {
    const [files, setFiles] = useState([]);
    const [pageInfo, setPageInfo] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [currentPage, setCurrentPage] = useState(0);

    const [expandedRows, setExpandedRows] = useState(new Set());
    const [versionsCache, setVersionsCache] = useState({});

    const [previewFile, setPreviewFile] = useState(null);

    const [showShareModal, setShowShareModal] = useState(false);
    const [shareFile, setShareFile] = useState(null);
    const [shareEmail, setShareEmail] = useState('');
    const [sharePermission, setSharePermission] = useState('READ');
    const [currentShares, setCurrentShares] = useState([]);
    const [shareError, setShareError] = useState('');

    const [showEditModal, setShowEditModal] = useState(false);
    const [editFile, setEditFile] = useState(null);
    const [newFilename, setNewFilename] = useState('');
    const [newContentType, setNewContentType] = useState('');
    const [editError, setEditError] = useState('');

    const [aiModalFile, setAiModalFile] = useState(null);
    const [showRagModal, setShowRagModal] = useState(false);

    // --- PERMISSIONS ---
    const canEdit = (file) => file.permission === 'OWNER' || file.permission === 'ADMIN' || file.permission === 'WRITE';
    const canShare = (file) => file.permission === 'OWNER' || file.permission === 'ADMIN';
    const canDelete = (file) => file.permission === 'OWNER' || file.permission === 'ADMIN';

    // --- LOAD FILES WITH UX IMPROVEMENT (UUID -> NAME) ---
    const fetchFiles = async (page) => {
        setIsLoading(true);
        setError('');
        try {
            const response = await axios.get(`${API_BASE_URL}/files?page=${page}&size=10&sort=createdAt,desc`);
            const data = response.data;
            let fetchedFiles = data.content || [];

            // Ottieni i nomi degli Owner per migliorare la UX
            const uniqueOwnerIds = [...new Set(fetchedFiles.map(f => f.owner))];
            const ownerMap = {};

            await Promise.all(uniqueOwnerIds.map(async (id) => {
                try {
                    const userRes = await axios.get(`${API_BASE_URL}/users/${id}`);
                    ownerMap[id] = userRes.data.fullName || userRes.data.email;
                } catch (e) {
                    ownerMap[id] = id; // Fallback all'UUID se non trovato
                }
            }));

            // Mappa i file aggiungendo il campo ownerName
            fetchedFiles = fetchedFiles.map(f => ({
                ...f,
                ownerName: ownerMap[f.owner] || f.owner
            }));

            setFiles(fetchedFiles);
            setPageInfo({
                totalPages: data.totalPages,
                number: data.number,
                first: data.first,
                last: data.last
            });

            if (expandedRows.size > 0) {
                expandedRows.forEach(fileId => fetchVersions(fileId));
            }

        } catch (err) {
            setError(err.response?.data?.detail || "Failed to load files.");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchFiles(currentPage);
    }, [currentPage]);

    const fetchVersions = async (fileId) => {
        try {
            const response = await axios.get(`${API_BASE_URL}/files/${fileId}/history`);
            setVersionsCache(prev => ({ ...prev, [fileId]: response.data }));
        } catch (err) {
            console.error("Error fetching versions", err);
        }
    };

    const toggleRow = (fileId) => {
        const newExpanded = new Set(expandedRows);
        if (newExpanded.has(fileId)) {
            newExpanded.delete(fileId);
        } else {
            newExpanded.add(fileId);
            fetchVersions(fileId);
        }
        setExpandedRows(newExpanded);
    };

    const deleteFile = async (id) => {
        if (!window.confirm("Are you sure you want to delete this file?")) return;
        try {
            await axios.delete(`${API_BASE_URL}/files/${id}`);
            fetchFiles(currentPage);
        } catch (err) {
            alert("Delete failed: " + (err.response?.data?.detail || err.message));
        }
    };

    const rollbackVersion = async (id) => {
        if (!window.confirm("Are you sure you want to rollback? This deletes the latest version.")) return;
        try {
            await axios.delete(`${API_BASE_URL}/files/${id}/versions/latest`);
            fetchFiles(currentPage);
            fetchVersions(id);
        } catch (err) {
            alert("Rollback failed: " + (err.response?.data?.detail || err.message));
        }
    };

    const openEditModal = (file) => {
        setEditFile(file);
        setNewFilename(file.filename);
        setNewContentType(file.contentType);
        setEditError('');
        setShowEditModal(true);
    };

    const handleSaveMetadata = async (e) => {
        e.preventDefault();
        setEditError('');
        try {
            await axios.patch(`${API_BASE_URL}/files/${editFile.id}`, {
                filename: newFilename,
                contentType: newContentType
            });
            setShowEditModal(false);
            fetchFiles(currentPage);
        } catch (err) {
            setEditError(err.response?.data?.detail || err.message);
        }
    };

    const openShareModal = async (file) => {
        setShareFile(file);
        setShowShareModal(true);
        setShareError('');
        setShareEmail('');
        setCurrentShares([]);

        try {
            const response = await axios.get(`${API_BASE_URL}/files/${file.id}/shares`);
            const sharesWithDetails = await Promise.all(response.data.map(async (share) => {
                try {
                    const userRes = await axios.get(`${API_BASE_URL}/users/${share.sharedWithUserId}`);
                    return { ...share, email: userRes.data.email, name: userRes.data.fullName };
                } catch {
                    return share;
                }
            }));
            setCurrentShares(sharesWithDetails);
        } catch (err) {
            console.error("Failed to load shares", err);
        }
    };

    // --- FIX UX: Refactor pulito senza throw annidati ---
    const handleAddShare = async (e) => {
        e.preventDefault();
        setShareError('');

        const emailToShare = shareEmail.trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(emailToShare)) {
            setShareError("Please enter a valid email address.");
            return;
        }

        let userResponse;
        try {
            // Cerca l'utente per email
            userResponse = await axios.get(`${API_BASE_URL}/users/by-email/${emailToShare}`);
        } catch (err) {
            // Se l'utente non esiste, imposta l'errore ed esci
            setShareError(`User with email '${emailToShare}' not found.`);
            return;
        }

        try {
            // Se l'utente esiste, procedi con la condivisione
            const userIdToShare = userResponse.data.id;
            const response = await axios.post(`${API_BASE_URL}/files/${shareFile.id}/shares`, {
                userId: userIdToShare,
                permission: sharePermission
            });

            const newShare = response.data;
            newShare.email = emailToShare;
            newShare.name = userResponse.data.fullName || '';

            setCurrentShares([...currentShares, newShare]);
            setShareEmail('');
        } catch (err) {
            setShareError(err.response?.data?.detail || err.message || "Share failed.");
        }
    };

    const handleDeleteShare = async (shareId) => {
        try {
            await axios.delete(`${API_BASE_URL}/files/${shareFile.id}/shares/${shareId}`);
            setCurrentShares(currentShares.filter(s => s.id !== shareId));
        } catch (err) {
            alert("Revoke failed");
        }
    };

    // --- MANAGE RETRY AI FROM FRONTEND ---
    const handleRetryAi = async (fileId) => {
        try {
            await axios.post(`${API_BASE_URL}/files/${fileId}/ai-retry`);
            alert("AI processing re-triggered! Refresh the page in a few seconds.");
            fetchFiles(currentPage);
            setAiModalFile(null); // Chiudiamo la modale per invogliare l'utente a ricaricare la riga
        } catch (err) {
            alert("Failed to trigger AI retry: " + (err.response?.data?.detail || err.message));
        }
    };

    let paginationItems = [];
    if (pageInfo) {
        for (let number = 0; number < pageInfo.totalPages; number++) {
            paginationItems.push(
                <Pagination.Item key={number} active={number === pageInfo.number} onClick={() => setCurrentPage(number)}>
                    {number + 1}
                </Pagination.Item>,
            );
        }
    }

    if (isLoading && files.length === 0) {
        return <div className="text-center mt-5"><Spinner animation="border" /></div>;
    }

    return (
        <Card className="mt-4 shadow-sm">
            <Card.Header className="d-flex justify-content-between align-items-center">
                <h5 className="mb-0">File Browser</h5>
                <Button variant="outline-secondary" size="sm" onClick={() => fetchFiles(currentPage)}>Refresh</Button>
                <Button variant="outline-primary" onClick={() => setShowRagModal(true)}>
                    <ChatDots className="me-2" />
                    Chat Assistant
                </Button>
            </Card.Header>
            <Card.Body>
                {error && <Alert variant="danger">{error}</Alert>}

                <Table hover responsive className="align-middle">
                    <thead className="table-light">
                    <tr>
                        <th style={{ width: '40px' }}></th>
                        <th>Name</th>
                        <th>Owner</th>
                        <th>Permission</th>
                        <th>Size</th>
                        <th>Ver.</th>
                        <th className="text-end">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {files.map(file => (
                        <React.Fragment key={file.id}>
                            <tr>
                                <td>
                                    <Button variant="link" size="sm" className="p-0 text-dark" onClick={() => toggleRow(file.id)}>
                                        {expandedRows.has(file.id) ? <ChevronDown /> : <ChevronRight />}
                                    </Button>
                                </td>
                                <td className="align-middle">
                                    <div className="d-flex align-items-center">
                                        <div style={{ cursor: 'pointer' }} onClick={() => setPreviewFile(file)}>
                                            <FileIconOrPreview file={file} />
                                        </div>
                                        <div className="d-flex flex-column ms-2">
                                            <div className="d-flex align-items-center">
                                                <span className="fw-bold text-primary" style={{ cursor: 'pointer' }} onClick={() => setPreviewFile(file)}>
                                                    {file.filename}
                                                </span>
                                                <Button
                                                    variant="link"
                                                    className="p-0 ms-2 text-info d-flex align-items-center"
                                                    title="AI Summary"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        setAiModalFile(file);
                                                    }}
                                                    style={{ textDecoration: 'none' }}
                                                >
                                                    <InfoCircle size={16} />
                                                </Button>
                                            </div>
                                            <small className="text-muted">{file.contentType}</small>
                                        </div>
                                    </div>
                                </td>
                                <td>
                                    <small className="text-muted" style={{fontFamily: 'monospace', fontSize: '0.85em'}}>
                                        {file.ownerName || file.owner}
                                    </small>
                                </td>
                                <td>
                                    {file.permission === 'OWNER' && <Badge bg="success">Owner</Badge>}
                                    {file.permission === 'ADMIN' && <Badge bg="danger">Admin</Badge>}
                                    {file.permission === 'WRITE' && <Badge bg="warning" text="dark">Write</Badge>}
                                    {file.permission === 'READ' && <Badge bg="secondary">Read</Badge>}
                                </td>
                                <td>{formatBytes(file.size)}</td>
                                <td><Badge bg="info">{file.versionCount}</Badge></td>
                                <td className="text-end">
                                    <Button variant="outline-primary" size="sm" className="me-1" href={`${API_BASE_URL}/files/${file.id}/download`} target="_blank" title="Download">
                                        <Download />
                                    </Button>

                                    {canEdit(file) && (
                                        <>
                                            <Button variant="outline-secondary" size="sm" className="me-1" onClick={() => openEditModal(file)} title="Edit">
                                                <PencilSquare />
                                            </Button>
                                            <Button variant="outline-warning" size="sm" className="me-1" onClick={() => onStartVersionUpdate(file)} title="New Version">
                                                <Upload />
                                            </Button>
                                        </>
                                    )}

                                    {canShare(file) && (
                                        <Button variant="outline-info" size="sm" className="me-1" onClick={() => openShareModal(file)} title="Share">
                                            <Share />
                                        </Button>
                                    )}

                                    {(canEdit(file) && file.versionCount > 1) && (
                                        <Button variant="outline-dark" size="sm" className="me-1" onClick={() => rollbackVersion(file.id)} title="Rollback">
                                            <ArrowCounterclockwise />
                                        </Button>
                                    )}

                                    {canDelete(file) && (
                                        <Button variant="outline-danger" size="sm" onClick={() => deleteFile(file.id)} title="Delete">
                                            <Trash />
                                        </Button>
                                    )}
                                </td>
                            </tr>

                            {expandedRows.has(file.id) && (
                                <tr>
                                    <td colSpan="7" className="bg-light p-3">
                                        <h6>Version History</h6>
                                        {versionsCache[file.id] ? (
                                            versionsCache[file.id].map(ver => (
                                                <div key={ver.version} className="d-flex justify-content-between border-bottom py-1">
                                                        <span>
                                                            <Badge bg="secondary" className="me-2">v{ver.version}</Badge>
                                                            {ver.filename} ({formatBytes(ver.size)}) - {formatDate(ver.updatedAt)}
                                                        </span>
                                                    <a href={`${API_BASE_URL}/files/${file.id}/versions/${ver.version}/download`} target="_blank" rel="noreferrer">
                                                        Download
                                                    </a>
                                                </div>
                                            ))
                                        ) : (
                                            <Spinner animation="border" size="sm" />
                                        )}
                                    </td>
                                </tr>
                            )}
                        </React.Fragment>
                    ))}
                    </tbody>
                </Table>

                {pageInfo && pageInfo.totalPages > 1 && (
                    <Pagination className="justify-content-center mt-3">
                        <Pagination.Prev onClick={() => setCurrentPage(p => p - 1)} disabled={pageInfo.first} />
                        {paginationItems}
                        <Pagination.Next onClick={() => setCurrentPage(p => p + 1)} disabled={pageInfo.last} />
                    </Pagination>
                )}
            </Card.Body>

            <Modal show={showShareModal} onHide={() => setShowShareModal(false)}>
                <Modal.Header closeButton><Modal.Title>Share File</Modal.Title></Modal.Header>
                <Modal.Body>
                    {shareError && <Alert variant="danger">{shareError}</Alert>}
                    <Form onSubmit={handleAddShare}>
                        <InputGroup className="mb-3">
                            <Form.Control
                                type="email"
                                placeholder="Enter user email (e.g. staff@staff.com)"
                                value={shareEmail}
                                onChange={e => setShareEmail(e.target.value)}
                                required
                            />
                            <Form.Select
                                style={{ maxWidth: '100px' }}
                                value={sharePermission}
                                onChange={e => setSharePermission(e.target.value)}
                            >
                                <option value="READ">Read</option>
                                <option value="WRITE">Write</option>
                            </Form.Select>
                            <Button type="submit">Share</Button>
                        </InputGroup>
                    </Form>
                    <hr />
                    <h6>Active Shares</h6>
                    <Table size="sm">
                        <tbody>
                        {currentShares.length === 0 ? <tr><td className="text-muted">No shares yet.</td></tr> :
                            currentShares.map(s => (
                                <tr key={s.id}>
                                    <td>{s.name ? `${s.name} (${s.email})` : s.sharedWithUserId}</td>
                                    <td><Badge bg={s.permission === 'WRITE' ? 'warning' : 'secondary'}>{s.permission}</Badge></td>
                                    <td className="text-end">
                                        <Button size="sm" variant="link" className="text-danger" onClick={() => handleDeleteShare(s.id)}>Revoke</Button>
                                    </td>
                                </tr>
                            ))
                        }
                        </tbody>
                    </Table>
                </Modal.Body>
            </Modal>

            <Modal show={showEditModal} onHide={() => setShowEditModal(false)}>
                <Modal.Header closeButton><Modal.Title>Edit Metadata</Modal.Title></Modal.Header>
                <Modal.Body>
                    {editError && <Alert variant="danger">{editError}</Alert>}
                    <Form onSubmit={handleSaveMetadata}>
                        <Form.Group className="mb-3">
                            <Form.Label>Filename</Form.Label>
                            <Form.Control type="text" value={newFilename} onChange={e => setNewFilename(e.target.value)} required />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Content Type</Form.Label>
                            <Form.Control type="text" value={newContentType} onChange={e => setNewContentType(e.target.value)} />
                        </Form.Group>
                        <Button type="submit" className="w-100">Save Changes</Button>
                    </Form>
                </Modal.Body>
            </Modal>

            <FilePreviewModal show={!!previewFile} onHide={() => setPreviewFile(null)} file={previewFile} />

            <AiMetadataModal
                show={!!aiModalFile}
                onHide={() => setAiModalFile(null)}
                file={aiModalFile}
                onRetryAi={handleRetryAi}
            />

            <RagChatModal
                show={showRagModal}
                onHide={() => setShowRagModal(false)}
                files={files}
            />
        </Card>
    );
}

export default FileBrowser;