package iuh.fit.auth.application.dto.request.user;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record SavePlayerSportProfileCommand(
        SportType sportType,
        SkillLevel skillLevel,
        Set<String> preferredPositions,
        String playStyle,
        String city,
        Double latitude,
        Double longitude,
        Double playRadiusKm,
        List<SaveAvailabilityCommand> availabilities
) {
    public void validate() throws ServiceException {
        if (sportType == null || skillLevel == null) {
            throw new ServiceException("Môn thể thao và trình độ không được để trống");
        }
        if (city == null || city.isBlank()) {
            throw new ServiceException("Vui lòng chọn tỉnh/thành phố cho hồ sơ");
        }
        if (city.trim().length() > 100) {
            throw new ServiceException("Tên tỉnh/thành phố không được vượt quá 100 ký tự");
        }
        // Scouting tính khoảng cách từ tọa độ này, nên hồ sơ luôn cần một điểm (mặc định: trung tâm tỉnh/thành).
        if (latitude == null || longitude == null) {
            throw new ServiceException("Thiếu tọa độ khu vực chơi");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new ServiceException("Vĩ độ phải nằm trong khoảng từ -90 đến 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new ServiceException("Kinh độ phải nằm trong khoảng từ -180 đến 180");
        }
        if (playRadiusKm != null && (playRadiusKm <= 0 || playRadiusKm > 200)) {
            throw new ServiceException("Bán kính chơi phải lớn hơn 0 và không vượt quá 200 km");
        }
        if (playStyle != null && playStyle.trim().length() > 255) {
            throw new ServiceException("Phong cách chơi không được vượt quá 255 ký tự");
        }

        Set<String> normalizedPositions = preferredPositions == null
                ? Set.of()
                : preferredPositions.stream()
                .filter(position -> position != null && !position.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
        if (normalizedPositions.size() > 10
                || normalizedPositions.stream().anyMatch(position -> position.length() > 50)) {
            throw new ServiceException("Tối đa 10 vị trí yêu thích, mỗi vị trí không quá 50 ký tự");
        }
        validateAvailabilities();
    }

    private void validateAvailabilities() throws ServiceException {
        if (availabilities == null) return;
        for (SaveAvailabilityCommand availability : availabilities) {
            if (availability == null) {
                throw new ServiceException("Lịch rảnh không hợp lệ");
            }
            availability.validate();
        }

        Map<Object, List<SaveAvailabilityCommand>> activeByDay = availabilities.stream()
                .filter(availability -> !Boolean.FALSE.equals(availability.active()))
                .collect(Collectors.groupingBy(SaveAvailabilityCommand::dayOfWeek));
        for (List<SaveAvailabilityCommand> dailySlots : activeByDay.values()) {
            List<SaveAvailabilityCommand> sorted = dailySlots.stream()
                    .sorted(Comparator.comparing(SaveAvailabilityCommand::startTime))
                    .toList();
            for (int index = 1; index < sorted.size(); index++) {
                if (sorted.get(index).startTime().isBefore(sorted.get(index - 1).endTime())) {
                    throw new ServiceException("Các khung giờ rảnh trong cùng một ngày không được chồng nhau");
                }
            }
        }
    }
}
