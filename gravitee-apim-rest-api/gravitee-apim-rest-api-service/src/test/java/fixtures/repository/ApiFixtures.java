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
package fixtures.repository;

import fixtures.definition.ApiDefinitionFixtures;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.model.Api;
import java.time.Instant;
import lombok.SneakyThrows;

public class ApiFixtures {

    private static final GraviteeMapper GRAVITEE_MAPPER = new GraviteeMapper();

    private ApiFixtures() {}

    private static final Api.ApiBuilder BASE = Api
        .builder()
        .id("api-id")
        .name("api-name")
        .description("api-description")
        .version("api-version")
        .definitionVersion(DefinitionVersion.V4)
        .createdAt(java.sql.Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
        .updatedAt(java.sql.Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
        .deployedAt(java.sql.Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));

    public static Api anApi() {
        return BASE.build();
    }

    @SneakyThrows
    public static Api aV2Api() {
        return BASE
            .definitionVersion(DefinitionVersion.V2)
            .definition(GRAVITEE_MAPPER.writeValueAsString(ApiDefinitionFixtures.anApiV2()))
            .build();
    }

    @SneakyThrows
    public static Api aV4Api() {
        return BASE
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .definition(GRAVITEE_MAPPER.writeValueAsString(ApiDefinitionFixtures.anApiV4()))
            .build();
    }

    @SneakyThrows
    public static Api aFederatedApi() {
        return BASE.definitionVersion(DefinitionVersion.FEDERATED).build();
    }
}
