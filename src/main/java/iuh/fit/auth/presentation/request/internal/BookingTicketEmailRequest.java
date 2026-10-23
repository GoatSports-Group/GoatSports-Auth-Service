package iuh.fit.auth.presentation.request.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingTicketEmailRequest(
        @NotNull UUID bookingId,
        @NotBlank @Size(max = 32) String bookingCode,
        @NotBlank @Size(max = 255) String venueName,
        @NotBlank @Size(max = 255) String courtName,
        @Size(max = 500) String venueAddress,
        @NotNull LocalDate playDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @PositiveOrZero Double depositAmount,
        @NotNull @PositiveOrZero Double remainingAmount,
        /** Token ký GSQR do venue-service cấp; auth chỉ vẽ thành ảnh, không đọc nội dung. */
        @NotBlank @Size(max = 1024) String qrCode
) {
}
