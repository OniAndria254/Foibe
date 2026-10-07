INSERT INTO churches (name, city, address, created_at, updated_at)
SELECT 'Fanavaozana', 'Antananarivo', NULL, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM churches);