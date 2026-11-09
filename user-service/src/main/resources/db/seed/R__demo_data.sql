-- Demo data for local development. Applied only with the "dev" profile.

INSERT IGNORE INTO `user` (id, name, surname, email, address, alerting, energy_alerting_threshold)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 10
)
SELECT n,
       CONCAT('User', n),
       CONCAT('Surname', n),
       CONCAT('user', n, '@example.com'),
       CONCAT(n, ' Example St'),
       n % 2 = 0,
       1000.0 + n
FROM seq;

INSERT IGNORE INTO device (id, name, type, location, user_id)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 200
)
SELECT n,
       CONCAT('Device', n),
       ELT((n % 6) + 1, 'SPEAKER', 'CAMERA', 'THERMOSTAT', 'LIGHT', 'LOCK', 'DOORBELL'),
       CONCAT('Location', (n % 3) + 1),
       (n % 10) + 1
FROM seq;