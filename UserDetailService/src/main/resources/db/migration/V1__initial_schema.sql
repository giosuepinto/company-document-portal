CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       full_name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       organization VARCHAR(255),
                       role VARCHAR(50) NOT NULL,
                       department VARCHAR(255),
                       storage_quota BIGINT,
                       notes TEXT,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP
);