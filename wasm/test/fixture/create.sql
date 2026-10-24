-- Fixture for the browser smoke test (test/index.html).
--
-- test/fixture/smoke.mv.db was created with the release H2 2.3.232 jar, which is
-- not committed. To regenerate it, drop h2-2.3.232.jar into this directory and
-- run from the wasm project root:
--
--   java -cp test/fixture/h2-2.3.232.jar org.h2.tools.RunScript `
--     -url "jdbc:h2:./test/fixture/smoke" -user sa -password "" `
--     -script test/fixture/create.sql
--
CREATE TABLE IF NOT EXISTS users(
    id INT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    score BIGINT DEFAULT 0
);
INSERT INTO users VALUES (1, 'Steve', 100), (2, 'Alex', 250), (3, 'Herobrine', 0);
CREATE TABLE IF NOT EXISTS meta(
    k VARCHAR(32) PRIMARY KEY,
    v VARCHAR(255)
);
INSERT INTO meta VALUES ('schema', '1'), ('generatedBy', 'h2-wasm smoke test');
