/**
 * Creating user entity for database
 */

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String role; // ADMIN or USER

    @Column(nullable = false)
    private BigDecimal walletBalance;

    public User() {}

    public User(String username, String role, BigDecimal walletBalance) {
        this.username = username;
        this.role = role;
        this.walletBalance = walletBalance;
    }

    // getters and setters
}