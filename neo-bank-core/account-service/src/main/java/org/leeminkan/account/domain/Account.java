package org.leeminkan.account.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Audited
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Account extends Auditable  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private BigDecimal balance;

    // BANKING MAGIC: Optimistic Locking
    // If two people update this row at once, the version mismatch will throw an error.
    @Version
    private Long version;
}