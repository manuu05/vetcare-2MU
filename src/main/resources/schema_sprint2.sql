-- SPRINT 2 DDL MIGRATION (19 SP: US-03, US-07, US-08, US-09, US-11)

PRAGMA foreign_keys = ON;

-- 1. Referente Primario di Contatto per ciascun Animale (US-03)
-- La PRIMARY KEY su animal_id garantisce un unico referente primario attivo per ogni paziente
CREATE TABLE IF NOT EXISTS animal_primary_contacts (
    animal_id INTEGER PRIMARY KEY,
    owner_id INTEGER NOT NULL,
    updated_at VARCHAR(30) NOT NULL,
    FOREIGN KEY (animal_id) REFERENCES animals(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

-- 2. Cartella Clinica Elettronica: Visite Mediche Consolidate e Storico Peso (US-08 & US-09)
CREATE TABLE IF NOT EXISTS medical_visits (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    animal_id INTEGER NOT NULL,
    visit_datetime VARCHAR(30) NOT NULL,
    veterinarian_name VARCHAR(100) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    anamnesis TEXT NOT NULL,
    objective_exam TEXT NOT NULL,
    weight_kg DOUBLE NOT NULL CHECK (weight_kg > 0),
    temperature_c DOUBLE NOT NULL CHECK (temperature_c > 30.0 AND temperature_c < 45.0),
    diagnosis TEXT NOT NULL,
    prescribed_therapy TEXT,
    consolidated INTEGER NOT NULL DEFAULT 1,
    digital_signature VARCHAR(140) NOT NULL,
    FOREIGN KEY (animal_id) REFERENCES animals(id) ON DELETE CASCADE
);

-- 3. Registro Audit Override Primario per Prescrizioni con Rischio Allergia (US-07)
CREATE TABLE IF NOT EXISTS prescription_overrides (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    animal_id INTEGER NOT NULL,
    prescribed_drug VARCHAR(120) NOT NULL,
    matched_allergen VARCHAR(120) NOT NULL,
    veterinarian_name VARCHAR(100) NOT NULL,
    clinical_justification TEXT NOT NULL,
    override_timestamp VARCHAR(30) NOT NULL,
    FOREIGN KEY (animal_id) REFERENCES animals(id) ON DELETE CASCADE
);

-- 4. Agenda Giornaliera e Settimanale Affiancata per i 3 Veterinari (US-11)
CREATE TABLE IF NOT EXISTS clinic_appointments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    animal_id INTEGER NOT NULL,
    veterinarian_id INTEGER NOT NULL CHECK (veterinarian_id IN (1, 2, 3)),
    visit_type VARCHAR(50) NOT NULL,
    start_datetime VARCHAR(30) NOT NULL,
    end_datetime VARCHAR(30) NOT NULL,
    clinical_notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PROGRAMMATO',
    FOREIGN KEY (animal_id) REFERENCES animals(id) ON DELETE CASCADE
);
