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
package io.gravitee.apim.core.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import io.gravitee.definition.model.v4.property.Property;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiTest {

    @Test
    void should_not_update_properties_for_api_v2() {
        assertThat(ApiFixtures.aProxyApiV2().updateDynamicProperties(List.of())).isFalse();
    }

    @Test
    void does_not_need_to_update() {
        final Api api = ApiFixtures.aProxyApiV4();
        api
            .getApiDefinitionHttpV4()
            .setProperties(List.of(new Property("key", "value", false, false), new Property("dynamic", "value", false, true)));
        assertThat(
            api.updateDynamicProperties(List.of(new Property("key", "value", false, true), new Property("dynamic", "value", false, true)))
        )
            .isFalse();
        // keys must be sorted by natural order
        assertThat(api.getApiDefinitionHttpV4().getProperties()).extracting(Property::getKey).containsExactly("dynamic", "key");
    }

    @Test
    void needs_to_update() {
        final Api api = ApiFixtures.aProxyApiV4();
        api.getApiDefinitionHttpV4().setProperties(List.of(new Property("key", "value", false, false)));
        assertThat(
            api.updateDynamicProperties(
                List.of(
                    new Property("key", "value", false, true),
                    new Property("dynamic", "value", false, true),
                    new Property("X-Other", "value", false, true)
                )
            )
        )
            .isTrue();
        // keys must be sorted by natural order
        assertThat(api.getApiDefinitionHttpV4().getProperties()).extracting(Property::getKey).containsExactly("X-Other", "dynamic", "key");
    }
}
