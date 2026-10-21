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
package io.gravitee.rest.api.model.configuration.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class IdentityProviderClientSecretTest {

    @Test
    public void withoutClientSecret_should_keep_null() {
        assertThat(IdentityProviderClientSecret.withoutClientSecret(null)).isNull();
    }

    @Test
    public void withoutClientSecret_should_copy_the_map_and_drop_only_the_secret() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("clientId", "client-id");
        configuration.put(IdentityProviderClientSecret.CLIENT_SECRET, "stored-secret");

        Map<String, Object> omitted = IdentityProviderClientSecret.withoutClientSecret(configuration);

        assertThat(omitted).containsEntry("clientId", "client-id").doesNotContainKey(IdentityProviderClientSecret.CLIENT_SECRET);
        assertThat(configuration).containsEntry(IdentityProviderClientSecret.CLIENT_SECRET, "stored-secret");
        assertThat(omitted).isNotSameAs(configuration);
    }

    @Test
    public void omitClientSecret_should_replace_the_entity_configuration_without_changing_the_previous_map() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("clientId", "client-id");
        configuration.put(IdentityProviderClientSecret.CLIENT_SECRET, "stored-secret");
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setConfiguration(configuration);

        IdentityProviderEntity omitted = IdentityProviderClientSecret.omitClientSecret(entity);

        assertThat(omitted).isSameAs(entity);
        assertThat(entity.getConfiguration()).containsEntry("clientId", "client-id").doesNotContainKey("clientSecret");
        assertThat(configuration).containsEntry(IdentityProviderClientSecret.CLIENT_SECRET, "stored-secret");
    }

    @Test
    public void omitClientSecret_should_keep_a_null_entity() {
        assertThat(IdentityProviderClientSecret.omitClientSecret(null)).isNull();
    }
}
