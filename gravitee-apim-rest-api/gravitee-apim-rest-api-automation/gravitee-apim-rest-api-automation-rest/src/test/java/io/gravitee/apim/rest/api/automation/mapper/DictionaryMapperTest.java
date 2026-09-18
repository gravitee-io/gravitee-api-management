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
import io.gravitee.apim.rest.api.automation.model.EncryptableValue;
import io.gravitee.apim.rest.api.automation.model.ManualDictionarySpec;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.configuration.dictionary.InvalidDictionaryPropertyOptionsException;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DictionaryMapperTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("organization-id", "environment-id");

    @Test
    void should_map_a_manifest_without_encrypted_properties_to_core_model() {
        DictionarySpec spec = manualSpec(Map.of("hostname", "api.example.com"), null);

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsExactly(new DictionaryProperty("hostname", "api.example.com", null, null));
    }

    @Test
    void should_map_an_encrypted_property_to_an_encryptable_core_property() {
        DictionarySpec spec = manualSpec(null, Map.of("secret", secret("plaintext")));

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsExactly(new DictionaryProperty("secret", "plaintext", null, true));
    }

    @Test
    void should_merge_plain_and_encrypted_properties_into_one_core_list() {
        DictionarySpec spec = manualSpec(Map.of("url", "https://backend"), Map.of("apiKey", secret("plaintext")));

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsExactlyInAnyOrder(
            new DictionaryProperty("url", "https://backend", null, null),
            new DictionaryProperty("apiKey", "plaintext", null, true)
        );
    }

    @Test
    void should_reject_a_key_declared_in_both_property_maps() {
        DictionarySpec spec = manualSpec(Map.of("apiKey", "plain"), Map.of("apiKey", secret("plaintext")));

        assertThatThrownBy(() -> DictionaryMapper.INSTANCE.toDictionary(spec))
            .isInstanceOf(InvalidDictionaryPropertyOptionsException.class)
            .hasMessageContaining("apiKey");
    }

    @Test
    void should_split_state_properties_by_encryption() {
        DictionaryEntity entity = manualEntity(Map.of("plain-key", "plain-value", "secret-key", "cipher"), "secret-key");

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(entity, EXECUTION_CONTEXT);

        assertThat(state.getManual().getProperties()).containsExactlyInAnyOrderEntriesOf(Map.of("plain-key", "plain-value"));
        assertThat(state.getManual().getEncryptedProperties()).containsOnlyKeys("secret-key");
        assertThat(state.getManual().getEncryptedProperties().get("secret-key").getValue()).isNull();
    }

    @Test
    void should_omit_encrypted_properties_when_a_dictionary_has_no_secret() {
        DictionaryEntity entity = manualEntity(Map.of("plain-key", "plain-value"), null);

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(entity, EXECUTION_CONTEXT);

        assertThat(state.getManual().getEncryptedProperties()).isNull();
        assertThat(state.getManual().getProperties()).containsExactlyInAnyOrderEntriesOf(Map.of("plain-key", "plain-value"));
    }

    @Test
    void should_round_trip_a_manifest_without_its_secret_values() {
        DictionarySpec spec = manualSpec(Map.of("plain-key", "plain-value"), Map.of("secret-key", secret("plaintext")));
        DictionaryEntity entity = manualEntity(Map.of("plain-key", "plain-value", "secret-key", "cipher"), "secret-key");

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(entity, EXECUTION_CONTEXT);

        assertThat(state.getManual().getProperties()).containsExactlyInAnyOrderEntriesOf(spec.getManual().getProperties());
        assertThat(state.getManual().getEncryptedProperties()).containsOnlyKeys(spec.getManual().getEncryptedProperties().keySet());
        assertThat(state.getManual().getEncryptedProperties().get("secret-key").getValue()).isNull();
    }

    @Test
    void should_not_echo_a_submitted_secret_value_on_dry_run() {
        DictionarySpec spec = manualSpec(Map.of("url", "https://backend"), Map.of("apiKey", secret("my secret API key")));

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(spec);

        assertThat(state.getManual().getEncryptedProperties()).containsOnlyKeys("apiKey");
        assertThat(state.getManual().getEncryptedProperties().get("apiKey").getValue()).isNull();
        assertThat(state.getManual().getProperties()).containsExactlyInAnyOrderEntriesOf(Map.of("url", "https://backend"));
    }

    @Test
    void should_map_manual_dictionary_without_properties() {
        DictionarySpec spec = manualSpec(null, null);

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).isNull();
    }

    private static DictionarySpec manualSpec(Map<String, String> properties, Map<String, EncryptableValue> encryptedProperties) {
        ManualDictionarySpec manual = new ManualDictionarySpec();
        manual.setProperties(properties);
        manual.setEncryptedProperties(encryptedProperties);
        return new DictionarySpec().type(DictionaryType.MANUAL).manual(manual);
    }

    private static EncryptableValue secret(String value) {
        return new EncryptableValue().value(value);
    }

    private static DictionaryEntity manualEntity(Map<String, String> properties, String encryptedKey) {
        return DictionaryEntity.builder()
            .id("dic-1")
            .name("My dic")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .properties(properties)
            .propertyOptions(
                encryptedKey == null ? Map.of() : Map.of(encryptedKey, DictionaryPropertyOptions.builder().encrypted(true).build())
            )
            .build();
    }
}
