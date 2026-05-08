CREATE TABLE patches (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    version    VARCHAR(50)  NOT NULL UNIQUE,
    notes      TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

