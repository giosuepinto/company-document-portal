CREATE TABLE outbox (
                        id UUID PRIMARY KEY,
                        aggregateid VARCHAR(255), -- ID del file o della versione
                        aggregatetype VARCHAR(255),         -- Tipo di evento (es. "FILE_UPLOADED")
                        payload JSONB,             -- Il contenuto del messaggio (fileId, versionId, ecc.)
                        created_at TIMESTAMP
);