package org.example.bookingservice.service;

import org.example.bookingservice.dto.*;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.Field;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.FieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class BookingService {

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private BookingRepository bookingRepository;

    public List<CourtGridDTO> getGridData(Long stadiumId, LocalDate date) {
        List<Field> fields = fieldRepository.findByStadiumId(stadiumId);
        if (fields.isEmpty()) return Collections.emptyList();

        List<Long> fieldIds = fields.stream().map(Field::getId).toList();
        List<Booking> bookings = bookingRepository.findByFieldIdInAndBookingDateAndStatusNot(
                fieldIds, date, Booking.BookingStatus.CANCELLED
        );

        List<CourtGridDTO> gridResult = new ArrayList<>();

        for (Field field : fields) {
            List<TimeSlotDTO> slots = new ArrayList<>();
            LocalTime current = LocalTime.of(5, 0);
            LocalTime endOfDay = LocalTime.of(23, 0);

            while (!current.isAfter(endOfDay)) {
                String slotStatus = "TRONG";

                for (Booking b : bookings) {
                    if (b.getFieldId().equals(field.getId())) {
                        if ((current.equals(b.getStartTime()) || current.isAfter(b.getStartTime()))
                                && current.isBefore(b.getEndTime())) {
                            slotStatus = mapStatus(b.getStatus());
                            break;
                        }
                    }
                }

                slots.add(new TimeSlotDTO(current.toString(), slotStatus));
                current = current.plusMinutes(30);
            }

            gridResult.add(new CourtGridDTO(field.getId(), field.getName(), slots));
        }

        return gridResult;
    }

    public Booking createBooking(BookingRequestDTO req) {
        Booking booking = Booking.builder()
                .fieldId(req.getSubCourtId()) // mapped từ subCourtId sang fieldId
                .userId(req.getUserId())
                .bookingDate(req.getBookingDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .totalPrice(req.getTotalPrice() != null ? req.getTotalPrice() : 0.0)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        return bookingRepository.save(booking);
    }

    private String mapStatus(Booking.BookingStatus status) {
        return switch (status) {
            case CONFIRMED -> "DA_DAT";
            case PENDING -> "PENDING";
            case CANCELLED -> "TRONG";
        };
    }
}