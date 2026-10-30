-- Run ONLY on the dedicated, approved staging database after Flyway creates its schema.
-- Run psql interactively with PSQL_HISTORY=/dev/null. Values below are BCrypt hashes,
-- not plaintext passwords. Get each from the offline PasswordHash terminal helper.
-- Never run with shell tracing or psql -a/-e. No existing data is removed.
\set ON_ERROR_STOP on
\prompt 'BCrypt hash for staging-c1: ' c1_hash
\prompt 'BCrypt hash for staging-c2: ' c2_hash
\prompt 'BCrypt hash for staging-c3: ' c3_hash
\prompt 'BCrypt hash for staging-advisor: ' advisor_hash
BEGIN;
CREATE TEMP TABLE bootstrap_hashes(username text, hash text, role text) ON COMMIT DROP;
INSERT INTO bootstrap_hashes VALUES
 ('staging-c1', :'c1_hash', 'CUSTOMER'),
 ('staging-c2', :'c2_hash', 'CUSTOMER'),
 ('staging-c3', :'c3_hash', 'CUSTOMER'),
 ('staging-advisor', :'advisor_hash', 'SERVICE_ADVISOR');
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM bootstrap_hashes WHERE hash !~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$')
 THEN RAISE EXCEPTION 'Expected BCrypt hashes'; END IF;
 IF EXISTS (SELECT 1 FROM users WHERE username IN ('customer1','customer2','customer3','customer4','advisor'))
 THEN RAISE EXCEPTION 'Demo database must not be used for cloud'; END IF;
END $$;
INSERT INTO users(username,password_hash,role) SELECT username,hash,role FROM bootstrap_hashes;
INSERT INTO motorcycles(owner_id,registration,model)
 SELECT id, 'STAGE-' || id, 'Synthetic Roadster' FROM users WHERE username IN ('staging-c1','staging-c2','staging-c3');
INSERT INTO service_centers(name) VALUES ('Private synthetic staging workshop');
INSERT INTO service_slots(center_id,starts_at,capacity)
 SELECT id, now() + n * interval '1 day', 2 FROM service_centers CROSS JOIN generate_series(1,10) n
 WHERE name='Private synthetic staging workshop';
COMMIT;
