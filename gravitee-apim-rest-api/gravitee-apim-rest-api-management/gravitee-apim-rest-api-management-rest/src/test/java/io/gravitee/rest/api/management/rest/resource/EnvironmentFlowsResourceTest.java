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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.EnvironmentFlowPhase;
import io.gravitee.rest.api.model.EnvironmentFlowStep;
import io.gravitee.rest.api.model.NewEnvironmentFlowEntity;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

public class EnvironmentFlowsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "flows";
    }

    @Test
    public void shouldCreateEnvironmentFlow() {
        final var newEnvironmentFlow = new NewEnvironmentFlowEntity();
        newEnvironmentFlow.setName("New Environment Flow");
        newEnvironmentFlow.setVersion("13");
        newEnvironmentFlow.setPhase(EnumSet.of(EnvironmentFlowPhase.REQUEST, EnvironmentFlowPhase.PUBLISH));
        var step = new EnvironmentFlowStep();
        step.setName("Step");
        step.setDescription("Description");
        step.setConfiguration(JsonNodeFactory.instance.objectNode());
        newEnvironmentFlow.setPolicies(List.of(step));

        final var response = envTarget().request().post(Entity.json(newEnvironmentFlow));

        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path("created-id").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }
}
