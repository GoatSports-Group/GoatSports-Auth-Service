package iuh.fit.auth.application.dto.request.user;

import iuh.fit.auth.application.exception.ServiceException;
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
    public void validate() throws ServiceException {
        if (dayOfWeek == null || startTime == null || endTime == null) {
            throw new ServiceException("Lịch rảnh phải có thứ, giờ bắt đầu và giờ kết thúc");
        }
        if (!startTime.isBefore(endTime)) {
            throw new ServiceException("Giờ bắt đầu phải sớm hơn giờ kết thúc");
        }
        if (timezone != null && timezone.trim().length() > 100) {
            throw new ServiceException("Múi giờ không được vượt quá 100 ký tự");
        }
    }
}
