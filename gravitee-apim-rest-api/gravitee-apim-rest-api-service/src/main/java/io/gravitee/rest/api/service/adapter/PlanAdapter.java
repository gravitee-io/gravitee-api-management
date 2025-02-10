package io.gravitee.rest.api.service.adapter;

import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.Locale;

@Mapper
public interface PlanAdapter {
    PlanAdapter INSTANCE = Mappers.getMapper(PlanAdapter.class);

    default PlanEntity map(GenericPlanEntity entity) {
        return switch (entity) {
            case PlanEntity p -> p;
            case io.gravitee.rest.api.model.v4.plan.PlanEntity v4 -> map(v4);
            case NativePlanEntity nativePlan -> map(nativePlan);
            default -> null;
        };
    }

    PlanEntity map(io.gravitee.rest.api.model.v4.plan.PlanEntity v4);
    PlanEntity map(NativePlanEntity v4);

    default PlanSecurityType map(PlanSecurity value) {
        return value == null ? null : PlanSecurityType.valueOf(value.getType().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
