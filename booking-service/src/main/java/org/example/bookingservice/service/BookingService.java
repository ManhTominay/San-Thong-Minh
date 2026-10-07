package org.example.bookingservice.service;

import org.example.bookingservice.dto.BookingRequestDTO;
import org.example.bookingservice.dto.CourtGridDTO;
import org.example.bookingservice.dto.TimeSlotDTO;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.Field;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.FieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FieldRepository fieldRepository;

    public List<CourtGridDTO> getGridData(Long stadiumId, LocalDate bookingDate) {
        List<Field> fields = fieldRepository.findByStadiumId(stadiumId);
        List<Long> fieldIds = fields.stream()
                .map(Field::getId)
                .collect(Collectors.toList());

        List<Booking> existingBookings = bookingRepository.findByFieldIdInAndBookingDateAndStatusNot(
                fieldIds, bookingDate, "CANCELLED"
        );

        List<CourtGridDTO> gridData = new ArrayList<>();

        for (Field field : fields) {
            CourtGridDTO courtGrid = new CourtGridDTO();
            courtGrid.setCourtId(field.getId());
            courtGrid.setCourtName(field.getName());

            List<TimeSlotDTO> timeSlots = new ArrayList<>();
            LocalTime start = LocalTime.of(5, 0);
            LocalTime end = LocalTime.of(23, 0);

            while (start.isBefore(end)) {
                LocalTime slotStart = start;
                LocalTime slotEnd = start.plusMinutes(30);

                boolean isBooked = existingBookings.stream().anyMatch(booking ->
                        booking.getFieldId().equals(field.getId())
                                && !(slotEnd.isBefore(booking.getStartTime())
                                || slotEnd.equals(booking.getStartTime())
                                || slotStart.isAfter(booking.getEndTime())
                                || slotStart.equals(booking.getEndTime()))
                );

                TimeSlotDTO slot = new TimeSlotDTO();
                slot.setStartTime(slotStart.toString());
                slot.setEndTime(slotEnd.toString());
                slot.setPrice(field.getPricePerHour() != null
                        ? field.getPricePerHour() / 2
                        : 35000.0);
                slot.setStatus(isBooked ? "BOOKED" : "AVAILABLE");

                timeSlots.add(slot);
                start = slotEnd;
            }

            courtGrid.setSlots(timeSlots);
            gridData.add(courtGrid);
        }

        return gridData;
    }

    public List<Booking> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserIdOrderByIdDesc(userId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional
    public Optional<Booking> updateBookingStatus(Long bookingId, String status) {
        return bookingRepository.findById(bookingId).map(booking -> {
            booking.setStatus(status);
            return bookingRepository.save(booking);
        });
    }

    @Transactional
    public Booking createBooking(BookingRequestDTO req) {
        Booking booking = new Booking();

        Long validUserId = req.getUserId() != null && req.getUserId() > 0
                ? req.getUserId()
                : 7L;
        booking.setUserId(validUserId);

        Long validFieldId = req.getFieldId() != null
                && req.getFieldId() >= 1
                && req.getFieldId() <= 18
                ? req.getFieldId()
                : 5L;
        booking.setFieldId(validFieldId);

        booking.setBookingDate(req.getBookingDate());
        booking.setStartTime(req.getStartTime());
        booking.setEndTime(req.getEndTime());
        booking.setTotalPrice(req.getTotalPrice() != null ? req.getTotalPrice() : 0.0);
        booking.setStatus(req.getStatus() != null ? req.getStatus() : "PENDING");
        booking.setCreatedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }
}
