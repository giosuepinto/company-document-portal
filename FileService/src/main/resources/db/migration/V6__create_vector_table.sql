CREATE TABLE IF NOT EXISTS file_version_embeddings (
                                                       id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(384) -- 768 per Gemini, ma questa è una modifica per compatibilità con la base di OpenAI
    );