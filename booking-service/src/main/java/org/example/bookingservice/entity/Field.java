package org.example.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String type;

    @Column(name = "price_per_hour", nullable = false)
    private Double pricePerHour;

    @Column(name = "stadium_id", nullable = false)
    private Long stadiumId;
}