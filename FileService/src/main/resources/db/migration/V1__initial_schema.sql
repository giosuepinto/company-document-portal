-- 1. Upload Table for temporary uploads
CREATE TABLE upload (
                        id UUID PRIMARY KEY,
                        filename VARCHAR(255) NOT NULL,
                        content_type VARCHAR(255) NOT NULL,
                        size BIGINT NOT NULL,
                        checksum VARCHAR(255),
                        status VARCHAR(50) NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        completed_at TIMESTAMP,
                        bucket VARCHAR(255) NOT NULL,
                        object_key VARCHAR(255) NOT NULL,
                        uploader_id VARCHAR(255),
                        merge_mode VARCHAR(50) NOT NULL,
                        chunk_count INT NOT NULL,
                        total_offset BIGINT NOT NULL,
                        json_metadata TEXT
);

-- 2. File Table (Entity Logic - Step 2)
CREATE TABLE file (
                      id UUID PRIMARY KEY,
                      owner VARCHAR(255) NOT NULL,
                      created_at TIMESTAMP NOT NULL,
                      updated_at TIMESTAMP NOT NULL,
                      sharing_rules INT
);

-- 3. FileVersion Table (Immutable Snapshot  - Step 2)
CREATE TABLE file_version (
                              file_id UUID NOT NULL,
                              version BIGINT NOT NULL,
                              size BIGINT NOT NULL,
                              checksum VARCHAR(255),
                              content_type VARCHAR(255) NOT NULL,
                              filename VARCHAR(255) NOT NULL,
                              updated_at TIMESTAMP NOT NULL,
                              minio_object_key VARCHAR(255) NOT NULL,

                              PRIMARY KEY (file_id, version),
                              CONSTRAINT fk_version_file FOREIGN KEY (file_id) REFERENCES file(id)
);

-- 4. FileShare Table (Step 4 - Sharing)
CREATE TABLE file_share (
                            id UUID PRIMARY KEY,
                            file_id UUID NOT NULL,
                            shared_with_user_id UUID NOT NULL,
                            permission VARCHAR(50) NOT NULL, -- READ, WRITE
                            created_at TIMESTAMP NOT NULL,

                            CONSTRAINT fk_share_file FOREIGN KEY (file_id) REFERENCES file(id)
);