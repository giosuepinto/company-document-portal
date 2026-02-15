import React, { useState, useEffect } from 'react';
import { Card, Alert, Spinner, Table, Button, Modal, Form, Row, Col, Pagination, Container, Badge } from 'react-bootstrap';
// AGGIUNGI L'IMPORT DI AXIOS
import axios from 'axios';
import { API_BASE_URL } from '../utils/config';

const API_USERS_URL = `${API_BASE_URL}/users`;

function AdminUsersPage() {
    const [users, setUsers] = useState([]);
    const [pageInfo, setPageInfo] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    // Modal states
    const [showModal, setShowModal] = useState(false);
    const [currentUser, setCurrentUser] = useState(null);
    const [editFormData, setEditFormData] = useState({});

    // --- LIFECYCLE ---
    useEffect(() => {
        fetchUsers(currentPage);
    }, [currentPage]);

    // --- API FUNCTIONS ---

    const fetchUsers = async (page) => {
        setIsLoading(true);
        setError('');
        try {
            // SOSTITUITO fetch CON axios.get
            const response = await axios.get(`${API_USERS_URL}?page=${page}&size=10&sort=email,asc`);

            // Axios restituisce i dati direttamente in .data
            const data = response.data;
            setUsers(data.content || []);
            setPageInfo({
                totalPages: data.totalPages,
                number: data.number,
                first: data.first,
                last: data.last
            });
        } catch (err) {
            // Gestione errori Axios
            const message = err.response?.data?.detail || err.message || "Failed to fetch users";
            setError(message);
            setUsers([]);
        } finally {
            setIsLoading(false);
        }
    };

    const handleEditClick = (user) => {
        setCurrentUser(user);
        setEditFormData({
            fullName: user.fullName,
            email: user.email,
            organization: user.organization,
            role: user.role,
            department: user.department || '',
            storageQuota: user.storageQuota || 0,
            notes: user.notes || ''
        });
        setShowModal(true);
    };

    const handleFormChange = (e) => {
        setEditFormData({
            ...editFormData,
            [e.target.id]: e.target.value
        });
    };

    const handleSaveUser = async (e) => {
        e.preventDefault();
        setError('');
        if (!currentUser) return;

        const payload = {
            fullName: editFormData.fullName,
            email: editFormData.email,
            organization: editFormData.organization,
            role: editFormData.role,
            department: editFormData.department,
            storageQuota: parseInt(editFormData.storageQuota) || 0,
            notes: editFormData.notes
        };

        try {
            // SOSTITUITO fetch CON axios.put
            // Axios gestisce automaticamente il CSRF Token e il Content-Type JSON
            const response = await axios.put(`${API_USERS_URL}/${currentUser.id}`, payload);

            const updatedUser = response.data;
            setUsers(users.map(u => (u.id === updatedUser.id ? updatedUser : u)));
            setShowModal(false);
        } catch (err) {
            const message = err.response?.data?.detail || `Update failed: ${err.message}`;
            setError(message);
        }
    };

    // --- RENDER ---
    // (Il resto del codice di rendering rimane identico)
    // ...
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

    return (
        <Container className="mt-4">
            <Card>
                <Card.Header as="h4" className="d-flex justify-content-between align-items-center">
                    <span>User Management (Business Data)</span>
                    <Button variant="outline-secondary" size="sm" onClick={() => fetchUsers(currentPage)}>Refresh</Button>
                </Card.Header>
                <Card.Body>
                    {error && <Alert variant="danger">{error}</Alert>}

                    {isLoading && users.length === 0 ? (
                        <div className="text-center mt-5"><Spinner animation="border" /></div>
                    ) : (
                        <>
                            <Table striped bordered hover responsive size="sm" className="align-middle">
                                <thead>
                                <tr>
                                    <th>Full Name</th>
                                    <th>Email</th>
                                    <th>Role (Internal)</th>
                                    <th>Dept</th>
                                    <th>Quota</th>
                                    <th className="text-center">Edit</th>
                                </tr>
                                </thead>
                                <tbody>
                                {users.map(user => (
                                    <tr key={user.id}>
                                        <td>{user.fullName}</td>
                                        <td>{user.email}</td>
                                        <td><Badge bg={user.role === 'ADMIN' ? 'danger' : user.role === 'MANAGER' ? 'warning' : 'info'}>{user.role}</Badge></td>
                                        <td>{user.department || '-'}</td>
                                        <td>{user.storageQuota ? (user.storageQuota / 1024 / 1024).toFixed(0) + ' MB' : 'Unlimited'}</td>
                                        <td className="text-center">
                                            <Button variant="outline-primary" size="sm" onClick={() => handleEditClick(user)}>Edit</Button>
                                        </td>
                                    </tr>
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
                        </>
                    )}
                </Card.Body>
            </Card>

            <Modal show={showModal} onHide={() => setShowModal(false)}>
                <Modal.Header closeButton><Modal.Title>Edit User</Modal.Title></Modal.Header>
                <Modal.Body>
                    <Form onSubmit={handleSaveUser}>
                        <Row className="mb-3">
                            <Col><Form.Label>Name</Form.Label><Form.Control id="fullName" type="text" value={editFormData.fullName} onChange={handleFormChange} required /></Col>
                            <Col><Form.Label>Email</Form.Label><Form.Control id="email" type="email" value={editFormData.email} disabled /></Col>
                        </Row>
                        <Row className="mb-3">
                            <Col><Form.Label>Organization</Form.Label><Form.Control id="organization" type="text" value={editFormData.organization} onChange={handleFormChange} /></Col>
                            <Col>
                                <Form.Label>Internal Role</Form.Label>
                                <Form.Control as="select" id="role" value={editFormData.role} onChange={handleFormChange}>
                                    <option value="STAFF">STAFF</option>
                                    <option value="MANAGER">MANAGER</option>
                                    <option value="ADMIN">ADMIN</option>
                                </Form.Control>
                            </Col>
                        </Row>
                        <Row className="mb-3">
                            <Col><Form.Label>Department</Form.Label><Form.Control id="department" type="text" value={editFormData.department} onChange={handleFormChange} /></Col>
                            <Col><Form.Label>Quota (Bytes)</Form.Label><Form.Control id="storageQuota" type="number" value={editFormData.storageQuota} onChange={handleFormChange} /></Col>
                        </Row>
                        <Form.Group className="mb-3"><Form.Label>Notes</Form.Label><Form.Control id="notes" as="textarea" rows={2} value={editFormData.notes} onChange={handleFormChange} /></Form.Group>
                        <Button variant="primary" type="submit" className="w-100">Save Changes</Button>
                    </Form>
                </Modal.Body>
            </Modal>
        </Container>
    );
}

export default AdminUsersPage;