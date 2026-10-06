package org.example.bookingservice.repository;

import org.example.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Lấy danh sách đặt sân để hiển thị trên ô lưới (Grid) theo danh sách fieldId và ngày
     */
    List<Booking> findByFieldIdInAndBookingDateAndStatusNot(
            List<Long> fieldIds,
            LocalDate bookingDate,
            Booking.BookingStatus status
    );

    /**
     * Lấy danh sách lịch sử đặt sân của một người dùng (Sắp xếp mới nhất lên đầu)
     */
    List<Booking> findByUserIdOrderByIdDesc(Long userId);

    /**
     * Lấy danh sách đặt sân của người dùng theo trạng thái
     */
    List<Booking> findByUserIdAndStatus(Long userId, Booking.BookingStatus status);

    /**
     * Kiểm tra xem sân đã có lịch đặt trùng khung giờ hay chưa
     */
    boolean existsByFieldIdAndBookingDateAndStatusNot(
            Long fieldId,
            LocalDate bookingDate,
            Booking.BookingStatus status
    );
}