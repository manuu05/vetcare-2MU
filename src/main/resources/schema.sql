-- ============================================================================
-- VETCARE - SCRIPT DI MIGRAZIONE DDL INIZIALE (TK-01 & TK-02)
-- Team Duff Tech (Gruppo 2MU) - Sprint 1
-- ============================================================================

PRAGMA foreign_keys = ON;

-- 1. Tabella Anagrafica Animali (US-01)
CREATE TABLE IF NOT EXISTS animals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    medical_record_code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    species VARCHAR(60) NOT NULL,
    breed VARCHAR(80),
    birth_date VARCHAR(20) NOT NULL,
    sex VARCHAR(10) NOT NULL,
    microchip VARCHAR(15) NOT NULL UNIQUE,
    weight DOUBLE NOT NULL CHECK (weight > 0),
    notes TEXT
);

-- 2. Tabella Anagrafica Proprietari (US-02)
CREATE TABLE IF NOT EXISTS owners (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    fiscal_code VARCHAR(16) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(120),
    preferred_channel VARCHAR(20) NOT NULL
);

-- 3. Tabella Junction N:M Animale_Proprietario (TK-02)
CREATE TABLE IF NOT EXISTS animal_owners (
    animal_id INTEGER NOT NULL,
    owner_id INTEGER NOT NULL,
    ownership_role VARCHAR(50) DEFAULT 'Comproprietario',
    PRIMARY KEY (animal_id, owner_id),
    FOREIGN KEY (animal_id) REFERENCES animals(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

-- 4. Tabella Allergie e Intolleranze Critiche (US-06)
CREATE TABLE IF NOT EXISTS animal_allergies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    animal_id INTEGER NOT NULL,
    substance VARCHAR(120) NOT NULL,
    severity VARCHAR(30) NOT NULL DEFAULT 'CRITICA',
    notes TEXT,
    FOREIGN KEY (animal_id) REFERENCES animals(id) ON DELETE CASCADE
);
