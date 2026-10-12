function bookingMatchesSport(booking, sport, stadiumId, stadiumName) {
    const normalize = value => String(value || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
    if (booking.stadiumId != null && stadiumId != null &&
        Number(booking.stadiumId) !== Number(stadiumId)) {
        return false;
    }
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

function markGridBookingRange(bookedSlots, timeSlots, courtId, startTime, endTime) {
    const toMinutes = value => {
        const match = String(value || "").match(/^(\d{1,2}):(\d{2})/);
        return match ? Number(match[1]) * 60 + Number(match[2]) : null;
    };
    const bookingStart = toMinutes(startTime);
    const bookingEnd = toMinutes(endTime);
    if (bookingStart === null || bookingEnd === null || bookingEnd <= bookingStart) return;

    timeSlots.forEach((slot, index) => {
        const slotStart = toMinutes(slot);
        if (slotStart === null) return;
        const nextSlotStart = index + 1 < timeSlots.length
            ? toMinutes(timeSlots[index + 1])
            : null;
        const slotEnd = nextSlotStart !== null && nextSlotStart > slotStart
            ? nextSlotStart
            : slotStart + 30;

        if (slotStart < bookingEnd && slotEnd > bookingStart) {
            bookedSlots.add(`${courtId}_${slot}`);
        }
    });
}

async function saveBookingRecord(bookingPayload) {
    const token = localStorage.getItem("token");
    if (!token) {
        throw new Error("Vui lòng đăng nhập lại trước khi đặt sân.");
    }

    const stadiumId = Number(bookingPayload.stadiumId);
    const requestedFieldId = Number(bookingPayload.fieldId);
    if (!Number.isSafeInteger(stadiumId) || stadiumId <= 0 ||
        !Number.isSafeInteger(requestedFieldId) || requestedFieldId <= 0) {
        throw new Error("Thông tin sân đặt không hợp lệ.");
    }

    const gridUrl = new URL("/api/bookings/grid", window.location.origin);
    gridUrl.searchParams.set("stadiumId", String(stadiumId));
    gridUrl.searchParams.set("date", bookingPayload.bookingDate);
    const gridResponse = await fetch(gridUrl);
    if (!gridResponse.ok) {
        throw new Error(`Không xác định được sân con để đặt (HTTP ${gridResponse.status}).`);
    }
    const courts = await gridResponse.json();
    if (!Array.isArray(courts)) {
        throw new Error("Dữ liệu sân con từ Booking Service không hợp lệ.");
    }
    const requestedCourt = courts.find(court => Number(court.courtId) === requestedFieldId);
    const resolvedFieldId = requestedCourt
        ? Number(requestedCourt.courtId)
        : courts.length > 0
            ? Number(courts[0].courtId)
            : requestedFieldId;
    if (!Number.isSafeInteger(resolvedFieldId) || resolvedFieldId <= 0) {
        throw new Error("Booking Service không trả về mã sân con hợp lệ.");
    }

    const response = await fetch("/api/bookings", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({ ...bookingPayload, fieldId: resolvedFieldId })
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
