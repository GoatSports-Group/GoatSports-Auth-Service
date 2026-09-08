package iuh.fit.auth.application.dto.request.user;

import iuh.fit.auth.domain.enums.DayOfWeek;

import java.time.LocalTime;
import java.util.UUID;

public record SaveAvailabilityCommand(
        UUID availabilityId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String timezone,
        Boolean active
) {
}
