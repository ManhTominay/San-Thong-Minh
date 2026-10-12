package org.example.bookingservice.service;

import org.example.bookingservice.dto.BookingRequestDTO;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

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
        Long stadiumId = field != null && field.getStadiumId() != null
                ? field.getStadiumId()
                : req.getFieldId();
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
        booking.setFieldId(req.getFieldId());
        booking.setFieldName(field != null
                ? firstNonBlank(field.getName(), stadium.name())
                : stadium.name());
        booking.setStadiumId(stadium.id());
        booking.setStadiumName(stadium.name());

        booking.setBookingDate(req.getBookingDate());
        booking.setStartTime(req.getStartTime());
        booking.setEndTime(req.getEndTime());
        booking.setTotalPrice(req.getTotalPrice());
        booking.setStatus(req.getStatus() != null && !req.getStatus().isBlank()
                ? req.getStatus().trim().toUpperCase()
                : "PENDING");

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
}
