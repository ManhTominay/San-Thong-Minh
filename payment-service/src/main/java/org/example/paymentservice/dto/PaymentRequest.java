package org.example.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    @NotNull
    @Positive
    private Long bookingId;

    @NotNull
    @Positive
    private Double amount;

    @NotBlank
    private String paymentMethod;

    @NotBlank
    private String transactionStatus;

    @NotBlank
    @Size(max = 2_800_000)
    private String paymentProof;
}