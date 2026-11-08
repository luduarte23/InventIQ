/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.util.Objects;

public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private Role role;
    private boolean active;

    public User(Long id, String username, String passwordHash, Role role, boolean active) {
        this.id = id;
        this.username = requireText(username, "username");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role es obligatorio");
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "role es obligatorio");
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = requireText(newPasswordHash, "passwordHash");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
