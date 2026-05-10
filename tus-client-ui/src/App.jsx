import React, { useState, useEffect } from 'react';
import { Routes, Route, Link, useNavigate, useLocation, Navigate } from 'react-router-dom';
import axios from 'axios'; // Usa axios per il check auth

// Import React-Bootstrap components
import Container from 'react-bootstrap/Container';
import Navbar from 'react-bootstrap/Navbar';
import Nav from 'react-bootstrap/Nav';
import Spinner from 'react-bootstrap/Spinner';
import Button from 'react-bootstrap/Button'; // Import Button

// Import Pages
import HomePage from './pages/HomePage.jsx';
import TusUploader from './pages/TusUploader.jsx';
import FileBrowser from './pages/FileBrowser.jsx';
import LoginPage from './pages/LoginPage.jsx';
import AdminUsersPage from './pages/AdminUsersPage.jsx';

// Import Config & Utils
import { API_BASE_URL } from './utils/config';
import { getCookie } from './utils/utils'; // Importa l'helper

// --- COMPONENTE PROTECTED ROUTE ---
const ProtectedRoute = ({ isAllowed, redirectPath = '/login', children }) => {
    if (!isAllowed) {
        return <Navigate to={redirectPath} replace />;
    }
    return children;
};

function App() {
    // Rimuovi navigate e location se non usati direttamente, o lasciali
    const navigate = useNavigate();

    // --- UPLOAD STATE (condiviso) ---
    const [file, setFile] = useState(null);
    const [progress, setProgress] = useState(0);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadUrl, setUploadUrl] = useState(null);
    const [error, setError] = useState('');
    const [chunkSize, setChunkSize] = useState(1024 * 1024);
    const [uploadMode, setUploadMode] = useState('full');
    const [fileIdToUpdate, setFileIdToUpdate] = useState(null);
    const uploadRef = React.useRef(null);

    // --- AUTH STATE ---
    const [currentUser, setCurrentUser] = useState(null);
    const [isLoadingAuth, setIsLoadingAuth] = useState(true);

    // 1. Check Auth al caricamento
    useEffect(() => {
        const checkAuth = async () => {
            try {
                // Axios userà automaticamente i cookie grazie alla config in main.jsx
                const response = await axios.get(`${API_BASE_URL}/users/me`);
                setCurrentUser(response.data);
            } catch (err) {
                console.log("Not logged in or session expired");
                setCurrentUser(null);
            } finally {
                setIsLoadingAuth(false);
            }
        };
        checkAuth();
    }, []);

    // 2. Funzione Logout Corretta (Fix Whitelabel Error)
    const handleLogout = (e) => {
        e.preventDefault();

        // Creiamo un form dinamicamente per inviare una richiesta POST standard
        // Questo permette al browser di seguire il redirect di Keycloak
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/logout'; // Usa il percorso relativo (gestito dal proxy)

        // Aggiungi il token CSRF come campo nascosto
        const csrfToken = getCookie('XSRF-TOKEN');
        if (csrfToken) {
            const hiddenField = document.createElement('input');
            hiddenField.type = 'hidden';
            hiddenField.name = '_csrf'; // Spring Security si aspetta questo nome nei form
            hiddenField.value = csrfToken;
            form.appendChild(hiddenField);
        }

        document.body.appendChild(form);
        form.submit();
    };

    // Reset stati upload
    const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        setFile(selectedFile);
        if (uploadRef.current) {
            uploadRef.current.abort();
            uploadRef.current = null;
        }
        setProgress(0);
        setUploadUrl(null);
        setError('');
        setIsUploading(false);
    };

    const handleCancelUpload = async () => {
        if (uploadRef.current) {
            try {
                await uploadRef.current.abort(true);
                setError('Upload cancelled by user.');
            } catch (err) {
                setError(`Failed to cancel: ${err.message}`);
            }
        }
        setIsUploading(false);
        setProgress(0);
        setFile(null);
        uploadRef.current = null;
    };

    // Callback per iniziare l'aggiornamento versione dal FileBrowser
    const handleStartVersionUpdate = (file) => {
        setFileIdToUpdate(file.id);
        // Resetta stati e naviga
        setFile(null);
        setError('');
        setProgress(0);
        setUploadUrl(null);
        navigate('/upload');
    };

    if (isLoadingAuth) {
        return <div className="d-flex justify-content-center align-items-center min-vh-100"><Spinner animation="border" /></div>;
    }

    const isLoggedIn = !!currentUser;
    const role = currentUser?.role?.toUpperCase();
    const isAdmin = role === 'ADMIN';
    const isManager = role === 'MANAGER';
    const isStaff = role === 'STAFF';
    // Permessi per upload e navigazione
    const canWrite = isAdmin || isManager || isStaff;

    return (
        <div className="bg-light min-vh-100 pb-5">
            <Navbar bg="dark" variant="dark" expand="lg" className="mb-4 shadow-sm">
                <Container>
                    <Navbar.Brand as={Link} to="/">File System</Navbar.Brand>
                    <Navbar.Toggle aria-controls="basic-navbar-nav" />
                    <Navbar.Collapse id="basic-navbar-nav">
                        <Nav className="me-auto">
                            <Nav.Link as={Link} to="/" active={location.pathname === '/'}>Home</Nav.Link>
                            {canWrite && <Nav.Link as={Link} to="/upload" active={location.pathname === '/upload'}>Upload</Nav.Link>}
                            {isLoggedIn && <Nav.Link as={Link} to="/files" active={location.pathname === '/files'}>Files</Nav.Link>}
                            {isAdmin && <Nav.Link as={Link} to="/admin" active={location.pathname === '/admin'}>Admin</Nav.Link>}
                        </Nav>
                        <Nav className="ms-auto align-items-center">
                            {isUploading && (
                                <span className="text-white me-3 d-flex align-items-center">
                                    <Spinner animation="grow" size="sm" variant="warning" className="me-2" />
                                    Uploading: {progress.toFixed(0)}%
                                </span>
                            )}
                            {isLoggedIn ? (
                                <>
                                    <Navbar.Text className="me-3 text-light">
                                        Signed in as: <strong>{currentUser.fullName || currentUser.email}</strong>
                                        <span className="ms-2 badge bg-secondary">{currentUser.role}</span>
                                    </Navbar.Text>
                                    {/* Usa un button vero per il logout */}
                                    <Button variant="link" onClick={handleLogout} className="text-danger fw-bold text-decoration-none">
                                        Logout
                                    </Button>
                                </>
                            ) : (
                                <Link to="/login" className="btn btn-outline-primary text-white ms-2 px-3">Login</Link>
                            )}
                        </Nav>
                    </Navbar.Collapse>
                </Container>
            </Navbar>

            <Container>
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    {/* Se loggato, /login manda alla home, altrimenti mostra la pagina di login */}
                    <Route path="/login" element={isLoggedIn ? <Navigate to="/" replace /> : <LoginPage />} />

                    <Route path="/files" element={
                        <ProtectedRoute isAllowed={isLoggedIn}>
                            <FileBrowser onStartVersionUpdate={handleStartVersionUpdate} currentUser={currentUser} />
                        </ProtectedRoute>
                    } />

                    <Route path="/upload" element={
                        <ProtectedRoute isAllowed={canWrite}>
                            <TusUploader
                                file={file} progress={progress} isUploading={isUploading}
                                uploadUrl={uploadUrl} error={error} chunkSize={chunkSize}
                                setChunkSize={setChunkSize} uploadMode={uploadMode}
                                setUploadMode={setUploadMode} handleFileChange={handleFileChange}
                                handleCancelUpload={handleCancelUpload} setError={setError}
                                fileIdToUpdate={fileIdToUpdate} setFileIdToUpdate={setFileIdToUpdate}
                                uploadRef={uploadRef} setIsUploading={setIsUploading} setProgress={setProgress} setUploadUrl={setUploadUrl} setFile={setFile}
                            />
                        </ProtectedRoute>
                    } />

                    <Route path="/admin" element={
                        <ProtectedRoute isAllowed={isAdmin}>
                            <AdminUsersPage />
                        </ProtectedRoute>
                    } />

                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </Container>
        </div>
    );
}

export default App;