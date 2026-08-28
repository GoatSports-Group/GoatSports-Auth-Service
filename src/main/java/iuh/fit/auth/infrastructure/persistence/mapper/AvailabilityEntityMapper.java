package iuh.fit.auth.infrastructure.persistence.mapper;

import iuh.fit.auth.domain.entity.Availability;
import iuh.fit.auth.infrastructure.persistence.entity.AvailabilityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AvailabilityEntityMapper {
    @Mapping(target = "profile", ignore = true)
    AvailabilityEntity toEntity(Availability domain);

    @Mapping(target = "profileId", source = "profile.profileId")
    Availability toDomain(AvailabilityEntity entity);
}
