package org.example.paymentservice.service.impl;

import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public Payment createPayment(PaymentRequest request) {
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionStatus(request.getTransactionStatus())
                .paymentProof(request.getPaymentProof())
                .paymentDate(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    @Override
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}