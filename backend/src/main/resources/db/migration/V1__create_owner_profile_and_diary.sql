CREATE TABLE owner_account (
    id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_owner_account_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE owner_session (
    token_hash BINARY(32) NOT NULL,
    owner_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (token_hash),
    INDEX ix_owner_session_owner (owner_id),
    CONSTRAINT fk_owner_session_owner FOREIGN KEY (owner_id) REFERENCES owner_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE owner_profile (
    owner_id BIGINT NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    gender VARCHAR(32) NOT NULL,
    birth_date DATE NOT NULL,
    occupation VARCHAR(200),
    routine TEXT,
    interests TEXT,
    mbti VARCHAR(16),
    zodiac VARCHAR(32),
    blood_type VARCHAR(16),
    self_intro TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (owner_id),
    CONSTRAINT fk_owner_profile_owner FOREIGN KEY (owner_id) REFERENCES owner_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE diary_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    entry_date DATE NOT NULL,
    body TEXT NOT NULL,
    archived_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_diary_entry_owner_date UNIQUE (owner_id, entry_date),
    CONSTRAINT fk_diary_entry_owner FOREIGN KEY (owner_id) REFERENCES owner_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
