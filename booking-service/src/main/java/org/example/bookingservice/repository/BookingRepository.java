package org.example.bookingservice.repository;

import org.example.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByFieldIdInAndBookingDateAndStatusNot(
            List<Long> fieldIds,
            LocalDate bookingDate,
            String status
    );

    List<Booking> findByBookingDateAndStatusNot(LocalDate bookingDate, String status);

    List<Booking> findByFieldIdAndBookingDateAndStatusNot(Long fieldId, LocalDate bookingDate, String status);

    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN Field f ON f.id = b.fieldId
            WHERE b.bookingDate = :bookingDate
              AND b.status <> :status
              AND (b.stadiumId = :stadiumId
                   OR f.stadiumId = :stadiumId
                   OR (b.stadiumId IS NULL AND b.fieldId = :stadiumId))
            """)
    List<Booking> findByStadiumIdAndBookingDate(
            @Param("stadiumId") Long stadiumId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("status") String status
    );

    List<Booking> findByUserIdOrderByIdDesc(Long userId);

    List<Booking> findByUserIdAndStatus(Long userId, String status);

    boolean existsByFieldIdAndBookingDateAndStatusNot(
            Long fieldId,
            LocalDate bookingDate,
            String status
    );
}