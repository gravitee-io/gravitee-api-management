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
package io.gravitee.apim.core.documentation.model;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public class ApiFreemarkerTemplateTest {

    @Test
    void should_convert_v4_api_to_api_freemarker_template() {
        var model = ApiFixtures.aMessageApiV4();

        var metadata = Map.of("meta", "data");
        var primaryOwner = new PrimaryOwnerEntity("id", "name", "displayName", PrimaryOwnerEntity.Type.USER);
        var api = new ApiFreemarkerTemplate(model, metadata, primaryOwner);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(api.getLifecycleState().toString()).isEqualTo(ApiLifecycleState.PUBLISHED.toString());
            soft.assertThat(api.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
            soft.assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
            soft.assertThat(api.getDeployedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
            soft.assertThat(api.getDescription()).isEqualTo("api-description");
            soft.assertThat(api.getId()).isEqualTo("my-api");
            soft.assertThat(api.getState().toString()).isEqualTo(LifecycleState.STARTED.toString());
            soft.assertThat(api.getName()).isEqualTo("My Api");
            soft.assertThat(api.getPicture()).isEqualTo("api-picture");
            soft.assertThat(api.getType()).isEqualTo(ApiType.MESSAGE);
            soft.assertThat(api.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
            soft.assertThat(api.getDeployedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
            soft.assertThat(api.getVisibility().toString()).isEqualTo(Visibility.PUBLIC.toString());
            soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
            soft.assertThat(api.getMetadata()).isEqualTo(metadata);
            soft.assertThat(api.getPrimaryOwner()).isEqualTo(primaryOwner);
        });
    }

    @Test
    void should_convert_v2_api_to_repository() {
        var model = ApiFixtures.aProxyApiV2();

        var metadata = Map.of("meta", "data");
        var primaryOwner = new PrimaryOwnerEntity("id", "name", "displayName", PrimaryOwnerEntity.Type.USER);
        var api = new ApiFreemarkerTemplate(model, metadata, primaryOwner);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(api.getLifecycleState().toString()).isEqualTo(ApiLifecycleState.PUBLISHED.toString());
            soft.assertThat(api.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
            soft.assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
            soft.assertThat(api.getDeployedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
            soft.assertThat(api.getDescription()).isEqualTo("api-description");
            soft.assertThat(api.getId()).isEqualTo("my-api");
            soft.assertThat(api.getState().toString()).isEqualTo(LifecycleState.STARTED.toString());
            soft.assertThat(api.getName()).isEqualTo("My Api");
            soft.assertThat(api.getPicture()).isEqualTo("api-picture");
            soft.assertThat(api.getType()).isEqualTo(ApiType.PROXY);
            soft.assertThat(api.getProxy()).isEqualTo(model.getApiDefinition().getProxy());
            soft.assertThat(api.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
            soft.assertThat(api.getDeployedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
            soft.assertThat(api.getVisibility().toString()).isEqualTo(Visibility.PUBLIC.toString());
            soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
            soft.assertThat(api.getMetadata()).isEqualTo(metadata);
            soft.assertThat(api.getPrimaryOwner()).isEqualTo(primaryOwner);
        });
    }
}
