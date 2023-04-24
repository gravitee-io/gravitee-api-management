/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.gravitee.gateway.api.service.ApiKey;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SingleApikeyDeployableTest {

    @Test
    void should_be_built_from_apiKey() {
        ApiKey apiKey = new ApiKey();
        apiKey.setId("id");
        apiKey.setApi("api");
        apiKey.setPlan("plan");
        SingleApiKeyDeployable apikeyDeployable = SingleApiKeyDeployable.builder().apiKey(apiKey).build();

        assertThat(apikeyDeployable.id()).isEqualTo("id");
        assertThat(apikeyDeployable.apiId()).isEqualTo("api");
        assertThat(apikeyDeployable.apiKey()).isEqualTo(apiKey);
        assertThat(apikeyDeployable.apiKeys()).isEqualTo(List.of(apiKey));
        assertThat(apikeyDeployable.apiKeyPlans()).isEqualTo(Set.of("plan"));
        assertThatThrownBy(() -> apikeyDeployable.apiKeys(List.of())).isInstanceOf(UnsupportedOperationException.class);
    }
}
