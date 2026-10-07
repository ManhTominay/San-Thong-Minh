package org.example.bookingservice.repository;

import org.example.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByFieldIdInAndBookingDateAndStatusNot(
            List<Long> fieldIds,
            LocalDate bookingDate,
            String status
    );

    List<Booking> findByUserIdOrderByIdDesc(Long userId);

    List<Booking> findByUserIdAndStatus(Long userId, String status);

    boolean existsByFieldIdAndBookingDateAndStatusNot(
            Long fieldId,
            LocalDate bookingDate,
            String status
    );
}