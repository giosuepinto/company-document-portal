ALTER TABLE file_version ADD COLUMN summary TEXT;
ALTER TABLE file_version ADD COLUMN sensitivity VARCHAR(255);

CREATE TABLE file_version_tags (
                                   file_id UUID NOT NULL,
                                   version INTEGER NOT NULL,
                                   tag VARCHAR(255),
                                   FOREIGN KEY (file_id, version) REFERENCES file_version(file_id, version)
);

ALTER TABLE file_version_tags REPLICA IDENTITY FULL;