package org.example.bookingservice.repository;

import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.dto.BookingStatisticsOverviewDTO;
import org.example.bookingservice.dto.DailyBookingStatisticsDTO;
import org.example.bookingservice.dto.FieldBookingStatisticsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :bookingId")
    java.util.Optional<Booking> findByIdForUpdate(@Param("bookingId") Long bookingId);

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

    @Query("""
            select new org.example.bookingservice.dto.BookingStatisticsOverviewDTO(
                count(b),
                coalesce(sum(case when upper(b.status) in ('CONFIRMED', 'APPROVED', 'SUCCESS') then 1L else 0L end), 0L),
                coalesce(sum(case when upper(b.status) = 'PENDING' then 1L else 0L end), 0L),
                coalesce(sum(case when upper(b.status) in ('CANCELLED', 'CANCELED') then 1L else 0L end), 0L),
                coalesce(sum(case when upper(b.status) is null
                                      or upper(b.status) not in ('CONFIRMED', 'APPROVED', 'SUCCESS', 'PENDING', 'CANCELLED', 'CANCELED')
                                 then 1L else 0L end), 0L),
                coalesce(sum(case when upper(b.status) in ('CONFIRMED', 'APPROVED', 'SUCCESS')
                                  then b.totalPrice else 0.0 end), 0.0)
            )
            from Booking b
            where b.bookingDate >= :fromDate and b.bookingDate <= :toDate
            """)
    BookingStatisticsOverviewDTO getStatisticsOverview(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            select new org.example.bookingservice.dto.DailyBookingStatisticsDTO(
                b.bookingDate,
                count(b),
                coalesce(sum(case when upper(b.status) in ('CONFIRMED', 'APPROVED', 'SUCCESS')
                                  then b.totalPrice else 0.0 end), 0.0)
            )
            from Booking b
            where b.bookingDate >= :fromDate and b.bookingDate <= :toDate
            group by b.bookingDate
            order by b.bookingDate
            """)
    List<DailyBookingStatisticsDTO> getDailyStatistics(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            select new org.example.bookingservice.dto.FieldBookingStatisticsDTO(
                b.fieldId,
                max(b.fieldName),
                max(b.stadiumName),
                count(b),
                coalesce(sum(case when upper(b.status) in ('CONFIRMED', 'APPROVED', 'SUCCESS')
                                  then b.totalPrice else 0.0 end), 0.0)
            )
            from Booking b
            where b.bookingDate >= :fromDate and b.bookingDate <= :toDate
            group by b.fieldId
            order by count(b) desc, sum(case when upper(b.status) in ('CONFIRMED', 'APPROVED', 'SUCCESS')
                                             then b.totalPrice else 0.0 end) desc
            """)
    List<FieldBookingStatisticsDTO> getTopFieldStatistics(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("select min(b.bookingDate) from Booking b")
    LocalDate findEarliestBookingDate();
}