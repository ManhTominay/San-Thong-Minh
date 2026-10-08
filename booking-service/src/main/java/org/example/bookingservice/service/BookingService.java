package org.example.bookingservice.service;

import org.example.bookingservice.dto.BookingRequestDTO;
import org.example.bookingservice.dto.BookingAvailabilityDTO;
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
import java.time.DateTimeException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Pattern COURT_TIME_PATTERN = Pattern.compile(
            "(\\d+)\\s*:\\s*(\\d{1,2})[h:](\\d{2})\\s*-\\s*(\\d{1,2})[h:](\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FieldRepository fieldRepository;

    public List<CourtGridDTO> getGridData(Long stadiumId, LocalDate bookingDate) {
        List<Field> fields = fieldRepository.findByStadiumId(stadiumId);
        List<Long> fieldIds = fields.stream().map(Field::getId).collect(Collectors.toList());

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

                boolean isBooked = existingBookings.stream().anyMatch(b ->
                        b.getFieldId().equals(field.getId()) &&
                                !(slotEnd.isBefore(b.getStartTime()) || slotEnd.equals(b.getStartTime()) ||
                                        slotStart.isAfter(b.getEndTime()) || slotStart.equals(b.getEndTime()))
                );

                TimeSlotDTO slot = new TimeSlotDTO();
                slot.setStartTime(slotStart.toString());
                slot.setEndTime(slotEnd.toString());
                slot.setPrice(field.getPricePerHour() != null ? field.getPricePerHour() / 2 : 35000.0);
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

    public List<BookingAvailabilityDTO> getBookingsByDate(LocalDate bookingDate, Long fieldId, Long stadiumId) {
        List<Booking> bookings;
        if (stadiumId != null) {
            bookings = bookingRepository.findByStadiumIdAndBookingDate(stadiumId, bookingDate, "CANCELLED");
        } else if (fieldId != null) {
            bookings = bookingRepository.findByFieldIdAndBookingDateAndStatusNot(fieldId, bookingDate, "CANCELLED");
        } else {
            bookings = bookingRepository.findByBookingDateAndStatusNot(bookingDate, "CANCELLED");
        }

        return bookings.stream().map(BookingAvailabilityDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public Booking createBooking(BookingRequestDTO req) {
        Booking booking = new Booking();

        Long validUserId = (req.getUserId() != null && req.getUserId() > 0) ? req.getUserId() : 7L;
        booking.setUserId(validUserId);

        Long validFieldId = req.getFieldId();
        if (req.getStadiumId() != null) {
            if (req.getStadiumId() <= 0) {
                throw new IllegalArgumentException("stadiumId không hợp lệ");
            }
            List<Field> stadiumFields = fieldRepository.findByStadiumId(req.getStadiumId());
            if (stadiumFields.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy sân con thuộc sân đã chọn");
            }
            Long requestedFieldId = validFieldId;
            boolean fieldBelongsToStadium = stadiumFields.stream()
                    .anyMatch(field -> field.getId().equals(requestedFieldId));
            if (!fieldBelongsToStadium) {
                validFieldId = stadiumFields.get(0).getId();
            }
            booking.setStadiumId(req.getStadiumId());
        }
        if (validFieldId == null || !fieldRepository.existsById(validFieldId)) {
            throw new IllegalArgumentException("fieldId không hợp lệ");
        }
        booking.setFieldId(validFieldId);

        booking.setBookingDate(req.getBookingDate());
        booking.setStartTime(req.getStartTime());
        booking.setEndTime(req.getEndTime());
        booking.setCourtSummary(req.getCourtSummary());
        validateBookingTime(req);
        validateNoCourtOverlap(req, validFieldId);
        booking.setStadiumName(req.getStadiumName());
        booking.setStadiumAddress(req.getStadiumAddress());
        booking.setTotalPrice(req.getTotalPrice() != null ? req.getTotalPrice() : 0.0);
        booking.setStatus(req.getStatus() != null ? req.getStatus() : "PENDING");
        booking.setCreatedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    private void validateBookingTime(BookingRequestDTO req) {
        if (req.getBookingDate() == null || req.getStartTime() == null || req.getEndTime() == null ||
                !req.getStartTime().isBefore(req.getEndTime())) {
            throw new IllegalArgumentException("Ngày hoặc giờ đặt sân không hợp lệ");
        }
    }

    private void validateNoCourtOverlap(BookingRequestDTO req, Long fieldId) {
        if (isCancelled(req.getStatus())) {
            return;
        }

        List<Booking> existingBookings = req.getStadiumId() != null
                ? bookingRepository.findByStadiumIdAndBookingDate(
                        req.getStadiumId(), req.getBookingDate(), "CANCELLED")
                : bookingRepository.findByFieldIdAndBookingDateAndStatusNot(
                        fieldId, req.getBookingDate(), "CANCELLED");
        List<BookingSlot> requestedSlots = getBookingSlots(
                req.getCourtSummary(), req.getStartTime(), req.getEndTime());

        for (Booking existing : existingBookings) {
            if (isCancelled(existing.getStatus()) ||
                    hasDifferentKnownSport(req.getCourtSummary(), existing.getCourtSummary())) {
                continue;
            }

            for (BookingSlot requested : requestedSlots) {
                for (BookingSlot booked : getBookingSlots(
                        existing.getCourtSummary(), existing.getStartTime(), existing.getEndTime())) {
                    if (requested.courtNumber() == booked.courtNumber() &&
                            requested.start().isBefore(booked.end()) &&
                            booked.start().isBefore(requested.end())) {
                        throw new IllegalArgumentException(
                                "Sân " + requested.courtNumber() + " vừa được đặt trùng giờ. " +
                                        "Vui lòng tải lại lịch và chọn khung giờ khác.");
                    }
                }
            }
        }
    }

    private List<BookingSlot> getBookingSlots(String summary, LocalTime fallbackStart, LocalTime fallbackEnd) {
        List<BookingSlot> slots = new ArrayList<>();
        if (summary != null && !summary.isBlank()) {
            for (String part : summary.split("[|,;\\n]+")) {
                Matcher matcher = COURT_TIME_PATTERN.matcher(part);
                if (!matcher.find()) {
                    continue;
                }
                try {
                    LocalTime start = LocalTime.of(
                            Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
                    LocalTime end = LocalTime.of(
                            Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
                    if (!start.isBefore(end)) {
                        throw new IllegalArgumentException("Khung giờ trong thông tin sân không hợp lệ");
                    }
                    slots.add(new BookingSlot(Integer.parseInt(matcher.group(1)), start, end));
                } catch (DateTimeException | NumberFormatException e) {
                    throw new IllegalArgumentException("Khung giờ trong thông tin sân không hợp lệ", e);
                }
            }
        }

        if (slots.isEmpty()) {
            slots.add(new BookingSlot(1, fallbackStart, fallbackEnd));
        }
        return slots;
    }

    private boolean hasDifferentKnownSport(String requestedSummary, String existingSummary) {
        String requestedSport = getSportKey(requestedSummary);
        String existingSport = getSportKey(existingSummary);
        return requestedSport != null && existingSport != null && !requestedSport.equals(existingSport);
    }

    private String getSportKey(String summary) {
        if (summary == null) {
            return null;
        }
        String normalized = Normalizer.normalize(summary, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (normalized.matches("(?s).*(cau\\s*long|badminton).*")) return "badminton";
        if (normalized.matches("(?s).*(bong\\s*da|football|soccer).*")) return "football";
        if (normalized.matches("(?s).*(bong\\s*chuyen|volleyball).*")) return "volleyball";
        if (normalized.matches("(?s).*pickleball.*")) return "pickleball";
        if (normalized.matches("(?s).*(tennis|tenit).*")) return "tennis";
        return null;
    }

    private boolean isCancelled(String status) {
        if (status == null) {
            return false;
        }
        String normalized = Normalizer.normalize(status, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.contains("cancel") || normalized.contains("huy");
    }

    private record BookingSlot(int courtNumber, LocalTime start, LocalTime end) {}
}