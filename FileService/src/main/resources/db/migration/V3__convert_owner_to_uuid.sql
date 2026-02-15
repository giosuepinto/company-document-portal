-- File: src/main/resources/db/migration/V2__convert_owner_to_uuid.sql
BEGIN;

-- 1) Aggiunge una colonna temporanea di tipo uuid
ALTER TABLE "file" ADD COLUMN owner_tmp uuid;

-- 2) Copia i valori validi (stringhe che rispettano il formato UUID) nella colonna temporanea
UPDATE "file"
SET owner_tmp = owner::uuid
WHERE owner IS NOT NULL
  AND owner ~ '^[0-9a-fA-F0-9\-]{36}$';

-- (Opzionale, esegui manualmente prima di procedere se vuoi verificare righe problematiche)
-- SELECT id, owner FROM "file" WHERE owner IS NOT NULL AND owner !~ '^[0-9a-fA-F0-9\-]{36}$';

-- 3) Elimina la colonna vecchia e rinomina la temporanea
ALTER TABLE "file" DROP COLUMN owner;
ALTER TABLE "file" RENAME COLUMN owner_tmp TO owner;

-- 4) (Opzionale) Se sei sicuro che non ci siano NULL, imposta NOT NULL
-- ALTER TABLE "file" ALTER COLUMN owner SET NOT NULL;

COMMIT;
