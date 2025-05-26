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
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class IntegrationFixture {

    private IntegrationFixture() {}

    public static final Integration.ApiIntegration BASE = new Integration.ApiIntegration(
        "integration-id",
        "test-name",
        "integration-description",
        "test-provider",
        "my-env",
        Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()),
        Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()),
        Set.of()
    );

    public static final Integration.A2aIntegration BASE_A2A = new Integration.A2aIntegration(
        "a2a-integration-id",
        "A2A Integration",
        "A2A integration description",
        "A2A",
        "my-env",
        Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()),
        Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()),
        Set.of(),
        List.of(new Integration.A2aIntegration.WellKnownUrl("https://example.com/.well-known/agent.json"))
    );

    public static Integration.ApiIntegration anApiIntegration() {
        return BASE;
    }

    public static Integration.ApiIntegration anApiIntegration(String envId) {
        return BASE.withEnvironmentId(envId);
    }

    public static Integration.ApiIntegration withGroups(Integration.ApiIntegration integration, Set<String> groups) {
        return new Integration.ApiIntegration(
            integration.id(),
            integration.name(),
            integration.description(),
            integration.provider(),
            integration.environmentId(),
            integration.createdAt(),
            integration.updatedAt(),
            groups
        );
    }

    public static Integration.ApiIntegration withUpdatedAt(Integration.ApiIntegration integration, String date) {
        return new Integration.ApiIntegration(
            integration.id(),
            integration.name(),
            integration.description(),
            integration.provider(),
            integration.environmentId(),
            Instant.parse(date).atZone(ZoneId.systemDefault()),
            integration.updatedAt(),
            integration.groups()
        );
    }

    public static Integration.A2aIntegration anA2aIntegration() {
        return BASE_A2A;
    }

    public static Integration.A2aIntegration anA2aIntegration(String envId) {
        return BASE_A2A.withEnvironmentId(envId);
    }

    public static Integration.A2aIntegration anA2aIntegrationWithWellKnownUrls(
        Collection<Integration.A2aIntegration.WellKnownUrl> wellKnownUrls
    ) {
        return new Integration.A2aIntegration(
            BASE_A2A.id(),
            BASE_A2A.name(),
            BASE_A2A.description(),
            BASE_A2A.provider(),
            BASE_A2A.environmentId(),
            BASE_A2A.createdAt(),
            BASE_A2A.updatedAt(),
            BASE_A2A.groups(),
            wellKnownUrls
        );
    }

    public static Integration.A2aIntegration anA2aIntegrationWithId(String id) {
        return new Integration.A2aIntegration(
            id,
            BASE_A2A.name(),
            BASE_A2A.description(),
            BASE_A2A.provider(),
            BASE_A2A.environmentId(),
            BASE_A2A.createdAt(),
            BASE_A2A.updatedAt(),
            BASE_A2A.groups(),
            BASE_A2A.wellKnownUrls()
        );
    }
}
