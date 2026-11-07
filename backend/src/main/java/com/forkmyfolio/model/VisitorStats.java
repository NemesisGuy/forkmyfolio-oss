package com.forkmyfolio.model;

import com.forkmyfolio.model.enums.VisitorStatType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "visitor_stats")
@Getter
@Setter
public class VisitorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100) // THE FIX: Ensure length is 50 to accommodate all enum names.
    private VisitorStatType type;

    @Column(nullable = false)
    private String refId;

    @Column(nullable = false)
    private long count = 0L;

    @UpdateTimestamp
    private Instant updatedAt;
}