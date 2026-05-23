-- Development-only users. Password for all seeded users: Password1!
INSERT INTO users (username, password)
VALUES
    ('user@example.com', '$2y$10$p3F2CXKSfYgsNAK4gFA5bOJCOlV8TNUVDoKiqj4C8Z2bNPi1AEWim'),
    ('admin@example.com', '$2y$10$p3F2CXKSfYgsNAK4gFA5bOJCOlV8TNUVDoKiqj4C8Z2bNPi1AEWim'),
    ('system@example.com', '$2y$10$p3F2CXKSfYgsNAK4gFA5bOJCOlV8TNUVDoKiqj4C8Z2bNPi1AEWim')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'USER'
FROM users
WHERE username = 'user@example.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN'
FROM users
WHERE username = 'admin@example.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'SYSTEM'
FROM users
WHERE username = 'system@example.com'
ON CONFLICT DO NOTHING;
