function bookingMatchesSport(booking, sport, stadiumId, stadiumName) {
    const normalize = value => String(value || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
    if (booking.stadiumId != null && stadiumId != null &&
        Number(booking.stadiumId) !== Number(stadiumId)) {
        return false;
    }
    const bookingName = normalize(booking.stadiumName).replace(/[^a-z0-9]/g, "");
    const expectedName = normalize(stadiumName).replace(/[^a-z0-9]/g, "");
    if (bookingName && expectedName && bookingName !== expectedName) return false;

    const summary = normalize(booking.courtSummary || booking.courtDetail);
    const expectedSport = normalize(sport);
    const sportPatterns = {
        badminton: /cau\s*long|badminton/,
        football: /bong\s*da|football|soccer/,
        volleyball: /bong\s*chuyen|volleyball/,
        pickleball: /pickleball/,
        tennis: /tennis|tenit/
    };
    const expectedKey = Object.keys(sportPatterns).find(key => sportPatterns[key].test(expectedSport));
    const detectedKey = Object.keys(sportPatterns).find(key => sportPatterns[key].test(summary));

    return !detectedKey || !expectedKey || detectedKey === expectedKey;
}

async function saveBookingRecord(bookingPayload) {
    const response = await fetch("/api/bookings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bookingPayload)
    });

    const responseText = await response.text();
    let savedBooking;
    try {
        savedBooking = responseText ? JSON.parse(responseText) : {};
    } catch (error) {
        savedBooking = responseText;
    }

    if (!response.ok) {
        const detail = typeof savedBooking === "string" ? savedBooking : savedBooking.message || savedBooking.error;
        throw new Error(detail || `Không lưu được lịch đặt sân (HTTP ${response.status}).`);
    }

    const bookingId = typeof savedBooking === "string" ? null : savedBooking.id || savedBooking.bookingId;
    if (!Number.isSafeInteger(Number(bookingId)) || Number(bookingId) <= 0) {
        throw new Error("Booking Service đã phản hồi nhưng không có mã đơn hợp lệ.");
    }

    return { id: Number(bookingId), bookingId: Number(bookingId) };
}
