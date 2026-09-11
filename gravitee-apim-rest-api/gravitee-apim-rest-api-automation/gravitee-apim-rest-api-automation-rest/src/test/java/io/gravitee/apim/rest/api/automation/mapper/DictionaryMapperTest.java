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
package io.gravitee.apim.rest.api.automation.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.dictionary.model.DictionaryProperty;
import io.gravitee.apim.rest.api.automation.model.DictionarySpec;
import io.gravitee.apim.rest.api.automation.model.DictionaryState;
import io.gravitee.apim.rest.api.automation.model.DictionaryType;
import io.gravitee.apim.rest.api.automation.model.ManualDictionarySpec;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DictionaryMapperTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("organization-id", "environment-id");

    @Test
    void should_map_plain_properties_to_unencrypted_core_properties() {
        DictionarySpec spec = new DictionarySpec()
            .type(DictionaryType.MANUAL)
            .manual(new ManualDictionarySpec().properties(Map.of("legacy-key", "legacy-value")));

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsEntry("legacy-key", new DictionaryProperty("legacy-value", false));
    }

    @Test
    void should_map_disjoint_properties_and_encrypted_properties_to_core_dictionary() {
        DictionarySpec spec = new DictionarySpec()
            .type(DictionaryType.MANUAL)
            .manual(
                new ManualDictionarySpec()
                    .properties(Map.of("plain-key", "plain-value"))
                    .encryptedProperties(Map.of("secret-key", "cipher"))
            );

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties())
            .containsEntry("plain-key", new DictionaryProperty("plain-value", false))
            .containsEntry("secret-key", new DictionaryProperty("cipher", true));
    }

    @Test
    void should_fail_loudly_when_a_key_appears_in_both_properties_and_encrypted_properties() {
        DictionarySpec spec = new DictionarySpec()
            .type(DictionaryType.MANUAL)
            .manual(
                new ManualDictionarySpec()
                    .properties(Map.of("duplicate-key", "plain-value"))
                    .encryptedProperties(Map.of("duplicate-key", "cipher"))
            );

        assertThatThrownBy(() -> DictionaryMapper.INSTANCE.toDictionary(spec)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_map_core_dictionary_with_mixed_encrypted_properties_to_state() {
        DictionaryEntity entity = DictionaryEntity.builder()
            .id("dic-1")
            .name("My dic")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .properties(Map.of("plain-key", "plain-value", "secret-key", "cipher"))
            .encryptedPropertyKeys(Set.of("secret-key"))
            .build();

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(entity, EXECUTION_CONTEXT);

        assertThat(state.getManual().getProperties()).containsEntry("plain-key", "plain-value");
        assertThat(state.getManual().getEncryptedProperties()).containsEntry("secret-key", "cipher");
        assertThat(state.getManual().getProperties()).doesNotContainKey("secret-key");
        assertThat(state.getManual().getEncryptedProperties()).doesNotContainKey("plain-key");
    }
}
