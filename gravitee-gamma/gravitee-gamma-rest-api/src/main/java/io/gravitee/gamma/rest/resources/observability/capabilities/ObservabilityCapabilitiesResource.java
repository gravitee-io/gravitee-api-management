/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gamma.rest.resources.observability.capabilities;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.observability.capabilities.use_case.GetObservabilityCapabilitiesUseCase;
import io.gravitee.gamma.rest.resources.observability.capabilities.dto.ObservabilityCapabilitiesDto;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;

/**
 * The caller's observability capabilities, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/capabilities}: which sections and actions a
 * UI may offer. No permission guard: the answer describes the caller's own access.
 *
 * @author GraviteeSource Team
 */
public class ObservabilityCapabilitiesResource {

    @Inject
    private GetObservabilityCapabilitiesUseCase getCapabilitiesUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ObservabilityCapabilitiesDto getCapabilities() {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getCapabilitiesUseCase.execute(
            new GetObservabilityCapabilitiesUseCase.Input(ctx.getOrganizationId(), ctx.getEnvironmentId())
        );
        return ObservabilityCapabilitiesDto.from(output.capabilities());
    }
}
