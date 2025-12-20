package org.leeminkan.bookstore.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant; // Good for storing universal time

@Entity
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) // The unique and non-nullable token string
    private String token;

    @Column(nullable = false)
    private Instant expiryDate; // When the token becomes invalid

    @OneToOne // Each refresh token belongs to one user (and we assume one token per user for now)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
}