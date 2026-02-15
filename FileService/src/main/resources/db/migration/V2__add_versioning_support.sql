-- Aggiungiamo la colonna per collegare un upload a un file esistente (per le versioni successive alla 1)
ALTER TABLE upload ADD COLUMN related_file_id UUID;

-- Aggiungiamo la colonna owner_id
ALTER TABLE upload ADD COLUMN owner_id UUID;