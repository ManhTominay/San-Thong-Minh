package org.example.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private Long bookingId;
    private Double amount;
    private String paymentMethod;
    private String transactionStatus;
    private String paymentProof; // Chuỗi Base64 gửi từ frontend
}