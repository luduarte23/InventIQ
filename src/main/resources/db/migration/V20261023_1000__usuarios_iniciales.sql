-- Usuarios iniciales para poder entrar y registrar ventas sin pasos manuales.
-- Contraseñas en BCrypt (sin prefijo {bcrypt}): admin / admin123 y cajero / cajero123.
-- PENDIENTE (A7): confirmar el PasswordEncoder; si se usa el delegante de Spring, hay que agregar el prefijo.
-- Si la app se despliega, se cambian estas contraseñas desde la administración.

INSERT INTO app_user (username, password_hash, role, active) VALUES
    ('admin',  '$2a$10$SzAV9Vl53wFXysF/bFx7r.MrKdJSgZkG4XeIgKcwPXcDWIieDqH0a', 'ADMIN',   TRUE),
    ('cajero', '$2a$10$XsSRaGADF1bjqi2bLfj92ONlSEuOpRW3AUEpybQyVENKi..j0ESbS', 'CASHIER', TRUE);
