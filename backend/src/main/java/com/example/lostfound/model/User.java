package com.example.lostfound.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique=true, nullable=false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password; // BCrypt hashed

    private String fullName;

    @Column(nullable=false)
    private String role; // "USER" or "ADMIN"

    @Builder.Default
private Instant createdAt = Instant.now();

}