package org.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    private Double amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_status")
    private String transactionStatus;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Lob
    @Column(name = "payment_proof", columnDefinition = "LONGTEXT")
    private String paymentProof;
}