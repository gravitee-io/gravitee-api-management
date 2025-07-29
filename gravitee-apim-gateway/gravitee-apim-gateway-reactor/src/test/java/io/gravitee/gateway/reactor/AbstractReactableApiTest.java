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
package io.gravitee.gateway.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.DefinitionVersion;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AbstractReactableApiTest {

    @Test
    void whatever_type_of_api_should_be_equal_to_another_api_with_same_id() {
        AbstractReactableApi<?> api1 = buildApiInteger("id1");
        AbstractReactableApi<?> api2 = buildApiDouble("id1");

        assertThat(api1).isEqualTo(api2);
    }

    @NotNull
    private static AbstractReactableApi<?> buildApiInteger(String id) {
        return new AbstractReactableApi<Integer>() {
            @Override
            public <D> Set<D> dependencies(Class<D> type) {
                return Set.of();
            }

            @Override
            public String getApiVersion() {
                return "";
            }

            @Override
            public DefinitionVersion getDefinitionVersion() {
                return null;
            }

            @Override
            public Set<String> getTags() {
                return Set.of();
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getName() {
                return "";
            }

            @Override
            public Set<String> getSubscribablePlans() {
                return Set.of();
            }

            @Override
            public Set<String> getApiKeyPlans() {
                return Set.of();
            }
        };
    }

    @NotNull
    private static AbstractReactableApi<?> buildApiDouble(String id) {
        return new AbstractReactableApi<Double>() {
            @Override
            public <D> Set<D> dependencies(Class<D> type) {
                return Set.of();
            }

            @Override
            public String getApiVersion() {
                return "";
            }

            @Override
            public DefinitionVersion getDefinitionVersion() {
                return null;
            }

            @Override
            public Set<String> getTags() {
                return Set.of();
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getName() {
                return "";
            }

            @Override
            public Set<String> getSubscribablePlans() {
                return Set.of();
            }

            @Override
            public Set<String> getApiKeyPlans() {
                return Set.of();
            }
        };
    }
}
