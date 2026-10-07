package org.example.paymentservice.service;

import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.entity.Payment;
import java.util.List;

public interface PaymentService {
    Payment createPayment(PaymentRequest request);
    Payment getPaymentByBookingId(Long bookingId);
    List<Payment> getAllPayments();
}