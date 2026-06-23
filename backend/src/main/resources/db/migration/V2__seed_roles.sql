-- V2: Seed initial owner account for first login (see tech/api.md login example)

INSERT INTO users (name, username, password_hash, role, status)
VALUES (
  '张老板',
  'admin',
  '$2b$10$8093WklOqwMEzAtM3K1BXuT74RfwwD6F93CTRpEdE1mM8oLLpVREi',
  'OWNER',
  'ACTIVE'
);
