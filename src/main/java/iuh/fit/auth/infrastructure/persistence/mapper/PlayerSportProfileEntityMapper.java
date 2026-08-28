package iuh.fit.auth.infrastructure.persistence.mapper;

import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.infrastructure.persistence.entity.PlayerSportProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AvailabilityEntityMapper.class)
public interface PlayerSportProfileEntityMapper {
    @Mapping(target = "user", ignore = true)
    PlayerSportProfileEntity toEntity(PlayerSportProfile domain);

    @Mapping(target = "userId", source = "user.userId")
    PlayerSportProfile toDomain(PlayerSportProfileEntity entity);
}
