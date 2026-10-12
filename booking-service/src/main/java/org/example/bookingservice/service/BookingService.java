package org.example.bookingservice.service;

import org.example.bookingservice.dto.BookingRequestDTO;
import org.example.bookingservice.dto.BookingAvailabilityDTO;
import org.example.bookingservice.dto.BookingResponseDTO;
import org.example.bookingservice.dto.BookingStatisticsDTO;
import org.example.bookingservice.dto.BookingStatisticsOverviewDTO;
import org.example.bookingservice.dto.CourtGridDTO;
import org.example.bookingservice.dto.DailyBookingStatisticsDTO;
import org.example.bookingservice.dto.FieldBookingStatisticsDTO;
import org.example.bookingservice.dto.StadiumSnapshotDTO;
import org.example.bookingservice.dto.TimeSlotDTO;
import org.example.bookingservice.dto.UserSnapshotDTO;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.Field;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.FieldRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Pattern COURT_TIME_PATTERN = Pattern.compile(
            "(\\d+)\\s*:\\s*(\\d{1,2})[h:](\\d{2})\\s*-\\s*(\\d{1,2})[h:](\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    private final BookingRepository bookingRepository;
    private final FieldRepository fieldRepository;
    private final RestTemplate restTemplate;

    @Value("${booking.integrations.user-service-url:http://localhost:8083}")
    private String userServiceUrl;

    @Value("${booking.integrations.stadium-service-url:http://localhost:8081}")
    private String stadiumServiceUrl;

    public BookingService(
            BookingRepository bookingRepository,
            FieldRepository fieldRepository,
            RestTemplate restTemplate) {
        this.bookingRepository = bookingRepository;
        this.fieldRepository = fieldRepository;
        this.restTemplate = restTemplate;
    }

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

    public List<BookingResponseDTO> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(BookingResponseDTO::from)
                .toList();
    }

    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingResponseDTO::from)
                .toList();
    }

    public BookingStatisticsDTO getStatistics(LocalDate fromDate, LocalDate toDate) {
        LocalDate resolvedToDate = toDate != null ? toDate : LocalDate.now();
        LocalDate resolvedFromDate = fromDate != null
                ? fromDate
                : bookingRepository.findEarliestBookingDate();
        if (resolvedFromDate == null) {
            resolvedFromDate = resolvedToDate;
        }
        if (fromDate != null && resolvedFromDate.isAfter(resolvedToDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu không được sau ngày kết thúc"
            );
        }
        if (resolvedFromDate.isAfter(resolvedToDate)) {
            resolvedFromDate = resolvedToDate;
        }

        BookingStatisticsOverviewDTO overview =
                bookingRepository.getStatisticsOverview(resolvedFromDate, resolvedToDate);
        List<DailyBookingStatisticsDTO> dailyBookings =
                bookingRepository.getDailyStatistics(resolvedFromDate, resolvedToDate);
        List<FieldBookingStatisticsDTO> topFields =
                bookingRepository.getTopFieldStatistics(
                        resolvedFromDate,
                        resolvedToDate,
                        PageRequest.of(0, 5)
                );

        return new BookingStatisticsDTO(
                resolvedFromDate,
                resolvedToDate,
                overview,
                dailyBookings,
                topFields
        );
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
    public Optional<BookingResponseDTO> updateBookingStatus(Long bookingId, String status) {
        return bookingRepository.findById(bookingId).map(booking -> {
            booking.setStatus(status);
            return BookingResponseDTO.from(bookingRepository.save(booking));
        });
    }

    public BookingResponseDTO createBooking(BookingRequestDTO req) {
        validateBookingRequest(req);

        UserSnapshotDTO user = fetchUser(req.getUserId());
        Field field = fieldRepository.findById(req.getFieldId()).orElse(null);
        Long requestedStadiumId = req.getStadiumId();

        if (requestedStadiumId != null) {
            if (requestedStadiumId <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stadiumId không hợp lệ");
            }

            List<Field> stadiumFields = fieldRepository.findByStadiumId(requestedStadiumId);
            if (stadiumFields.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân con thuộc sân đã chọn");
            }

            if (field == null || !requestedStadiumId.equals(field.getStadiumId())) {
                if (req.getFieldId().equals(requestedStadiumId)) {
                    field = stadiumFields.get(0);
                } else {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "fieldId không thuộc stadiumId đã chọn"
                    );
                }
            }
        }

        Long validFieldId = field != null ? field.getId() : req.getFieldId();
        Long stadiumId = field != null && field.getStadiumId() != null
                ? field.getStadiumId()
                : requestedStadiumId != null ? requestedStadiumId : req.getFieldId();
        StadiumSnapshotDTO stadium = fetchStadium(stadiumId);
        if (stadium.id() == null || stadium.name() == null || stadium.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Thông tin sân nhận được từ stadium-service không hợp lệ"
            );
        }

        Booking booking = new Booking();
        booking.setUserId(user.id());
        booking.setUserName(firstNonBlank(user.name(), user.username(), user.email()));
        booking.setUserEmail(user.email());
        booking.setFieldId(validFieldId);
        booking.setFieldName(field != null
                ? firstNonBlank(field.getName(), stadium.name())
                : stadium.name());
        booking.setStadiumId(stadium.id());
        booking.setStadiumName(stadium.name());

        booking.setBookingDate(req.getBookingDate());
        booking.setStartTime(req.getStartTime());
        booking.setEndTime(req.getEndTime());
        booking.setCourtSummary(req.getCourtSummary());
        booking.setStadiumAddress(req.getStadiumAddress());
        booking.setTotalPrice(req.getTotalPrice());
        booking.setStatus(req.getStatus() != null && !req.getStatus().isBlank()
                ? req.getStatus().trim().toUpperCase()
                : "PENDING");
        validateNoCourtOverlap(req, validFieldId, stadium.id());

        return BookingResponseDTO.from(bookingRepository.save(booking));
    }

    private UserSnapshotDTO fetchUser(Long userId) {
        String url = userServiceUrl + "/api/admin/users/" + userId;
        try {
            UserSnapshotDTO user = restTemplate.getForObject(url, UserSnapshotDTO.class);
            if (user == null || user.id() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "user-service trả về dữ liệu người dùng không hợp lệ"
                );
            }
            return user;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Không thể xác thực người dùng vì user-service hiện không khả dụng"
            );
        }
    }

    private StadiumSnapshotDTO fetchStadium(Long fieldId) {
        String url = stadiumServiceUrl + "/api/fields/" + fieldId;
        try {
            StadiumSnapshotDTO stadium = restTemplate.getForObject(url, StadiumSnapshotDTO.class);
            if (stadium == null || stadium.id() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "stadium-service trả về dữ liệu sân không hợp lệ"
                );
            }
            return stadium;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Không thể xác thực sân vì stadium-service hiện không khả dụng"
            );
        }
    }

    private void validateBookingRequest(BookingRequestDTO req) {
        if (req == null
                || req.getUserId() == null
                || req.getUserId() <= 0
                || req.getFieldId() == null
                || req.getFieldId() <= 0
                || req.getBookingDate() == null
                || req.getStartTime() == null
                || req.getEndTime() == null
                || !req.getStartTime().isBefore(req.getEndTime())
                || req.getTotalPrice() == null
                || req.getTotalPrice() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin đặt sân không hợp lệ hoặc còn thiếu"
            );
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void validateNoCourtOverlap(BookingRequestDTO req, Long fieldId, Long stadiumId) {
        if (isCancelled(req.getStatus())) {
            return;
        }

        List<Booking> existingBookings = stadiumId != null
                ? bookingRepository.findByStadiumIdAndBookingDate(
                        stadiumId, req.getBookingDate(), "CANCELLED")
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
