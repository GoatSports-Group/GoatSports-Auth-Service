package iuh.fit.auth.application.dto.response.user;

import iuh.fit.auth.domain.enums.DayOfWeek;

import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityResponseCommand(
        UUID availabilityId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String timezone,
        Boolean active
) {
}
