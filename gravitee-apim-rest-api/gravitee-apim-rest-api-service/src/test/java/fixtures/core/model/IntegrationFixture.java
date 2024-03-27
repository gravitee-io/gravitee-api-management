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
package fixtures.core.model;

import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

public class IntegrationFixture {

    private IntegrationFixture() {}

    public static final Supplier<Integration.IntegrationBuilder> BASE = () ->
        Integration
            .builder()
            .id(UuidString.generateRandom())
            .name("test-name")
            .description("integration-description")
            .provider("test-provider")
            .environmentId("my-env")
            .agentStatus(Integration.AgentStatus.DISCONNECTED)
            .createdAt(ZonedDateTime.parse("2020-02-03T20:22:02.00Z").withZoneSameLocal(ZoneId.systemDefault()))
            .updatedAt(ZonedDateTime.parse("2020-02-03T20:22:02.00Z").withZoneSameLocal(ZoneId.systemDefault()));

    public static Integration anIntegration() {
        return BASE.get().build();
    }

    public static Integration anIntegration(String envId) {
        return BASE.get().environmentId(envId).build();
    }
}
