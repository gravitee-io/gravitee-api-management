package io.gravitee.apim.core.log.model;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.plan.model.Plan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class ConnectionLog extends BaseConnectionLog {
    private Api api;
    private Plan plan;
}
