-- File: src/main/resources/db/migration/V4__add_preview_column.sql

-- Add the missing column for storing the preview image path in MinIO
ALTER TABLE file_version ADD COLUMN preview_minio_key VARCHAR(255);