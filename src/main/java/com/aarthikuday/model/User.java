package com.aarthikuday.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(name = "user_id", unique = true, nullable = false)
    private String username;

    @NotBlank
    @Size(min = 3, max = 100)
    @Column(name = "password", nullable = false)
    private String password;

    @NotBlank
    @Column(name = "role", nullable = false)
    private String role; // "BANK_USER" or "CUSTOMER" in DB, mapped to "ROLE_ADMIN"/"ROLE_CLIENT"

    @Column(name = "status")
    private String status = "ACTIVE"; // "ACTIVE" in your DB

    @Column(name = "srl_no")
    private Integer srlNo;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Transient
    private boolean active = true;

    public User() {
    }

    public User(String username, String password, String role, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        if ("BANK_USER".equalsIgnoreCase(this.role)) {
            return "ROLE_ADMIN";
        } else if ("CUSTOMER".equalsIgnoreCase(this.role)) {
            return "ROLE_CLIENT";
        }
        return role;
    }

    public void setRole(String role) {
        if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
            this.role = "BANK_USER";
        } else if ("ROLE_CLIENT".equalsIgnoreCase(role)) {
            this.role = "CUSTOMER";
        } else {
            this.role = role;
        }
    }

    public String getFullName() {
        return fullName != null ? fullName : username;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email != null ? email : (username + "@aarthikuday.com");
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        if (status != null) {
            return "ACTIVE".equalsIgnoreCase(status);
        }
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.status = active ? "ACTIVE" : "INACTIVE";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSrlNo() {
        return srlNo;
    }

    public void setSrlNo(Integer srlNo) {
        this.srlNo = srlNo;
    }
}
