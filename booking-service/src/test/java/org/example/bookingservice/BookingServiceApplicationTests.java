package org.example.bookingservice;

import org.example.bookingservice.dto.BookingAvailabilityDTO;
import org.example.bookingservice.entity.Booking;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BookingServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void availabilityResponseIncludesBookingId() {
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setStadiumId(8L);

        BookingAvailabilityDTO availability = new BookingAvailabilityDTO(booking);
        assertEquals(42L, availability.getId());
        assertEquals(8L, availability.getStadiumId());
    }

}
