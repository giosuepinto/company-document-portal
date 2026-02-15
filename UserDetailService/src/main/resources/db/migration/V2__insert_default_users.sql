-- Inserisce l'Admin (L'ID deve combaciare con quello nel file-management-realm.json di Keycloak!)
INSERT INTO users (id, email, full_name, role, organization, department, storage_quota)
VALUES
    ('c3031b73-7560-4655-a485-5af13621ffb8', 'admin@admin.com', 'System Admin', 'ADMIN', 'Polito', 'IT', 1073741824)
    ON CONFLICT (id) DO UPDATE
                            SET role = 'ADMIN'; -- Se esiste già, corregge il ruolo forzatamente

-- Inserisce il Manager (ID preso dal tuo realm json)
INSERT INTO users (id, email, full_name, role, organization, department, storage_quota)
VALUES
    ('09f8103a-5ea8-4c9e-9659-d57e4307655c', 'manager@manager.com', 'Office Manager', 'MANAGER', 'Polito', 'HR', 524288000)
    ON CONFLICT (id) DO UPDATE SET role = 'MANAGER';

-- Inserisce lo Staff (ID preso dal tuo realm json)
INSERT INTO users (id, email, full_name, role, organization, department, storage_quota)
VALUES
    ('24975d6a-3a35-477a-bffd-15cc83d74f30', 'staff@staff.com', 'Employee Staff', 'STAFF', 'Polito', 'HR', 104857600)
    ON CONFLICT (id) DO UPDATE SET role = 'STAFF';