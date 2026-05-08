ALTER TABLE accounts
    ADD COLUMN superuser BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE account_accesses (
    account_id  UUID         NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    access_node VARCHAR(100) NOT NULL,
    PRIMARY KEY (account_id, access_node)
);

