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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.rest.api.automation.model.DictionaryPropertyOptions;
import io.gravitee.apim.rest.api.automation.model.DictionarySpec;
import io.gravitee.apim.rest.api.automation.model.DictionaryState;
import io.gravitee.apim.rest.api.automation.model.DictionaryType;
import io.gravitee.apim.rest.api.automation.model.ManualDictionarySpec;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DictionaryMapperTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("organization-id", "environment-id");

    @Test
    void should_map_a_manifest_without_property_options_to_core_model() {
        DictionarySpec spec = manualSpec(Map.of("hostname", "api.example.com"), null);

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsExactly(new DictionaryProperty("hostname", "api.example.com", null, null));
    }

    @Test
    void should_map_an_encrypted_option_to_core_model() {
        DictionarySpec spec = manualSpec(Map.of("secret", "cipher"), Map.of("secret", options(true, null)));

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsExactly(new DictionaryProperty("secret", "cipher", true, null));
    }

    @Test
    void should_map_an_encryptable_option_to_core_model() {
        DictionarySpec spec = manualSpec(Map.of("secret", "plaintext"), Map.of("secret", options(null, true)));

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsExactly(new DictionaryProperty("secret", "plaintext", null, true));
    }

    @Test
    void should_reject_options_naming_a_property_that_is_not_declared() {
        DictionarySpec spec = manualSpec(Map.of("hostname", "api.example.com"), Map.of("ghost", options(true, null)));

        assertThatThrownBy(() -> DictionaryMapper.INSTANCE.toDictionary(spec))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("ghost");
    }

    @Test
    void should_report_options_only_for_encrypted_properties() {
        DictionaryEntity entity = DictionaryEntity.builder()
            .id("dic-1")
            .name("My dic")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .properties(Map.of("plain-key", "plain-value", "secret-key", "cipher"))
            .propertyOptions(
                Map.of(
                    "secret-key",
                    io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions.builder().encrypted(true).build()
                )
            )
            .build();

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(entity, EXECUTION_CONTEXT);

        assertThat(state.getManual().getProperties()).containsExactlyInAnyOrderEntriesOf(
            Map.of("plain-key", "plain-value", "secret-key", "cipher")
        );
        assertThat(state.getManual().getPropertyOptions()).containsOnlyKeys("secret-key");
        assertThat(state.getManual().getPropertyOptions().get("secret-key").getEncrypted()).isTrue();
    }

    @Test
    void should_round_trip_a_manifest_unchanged() {
        DictionarySpec spec = manualSpec(
            Map.of("plain-key", "plain-value", "secret-key", "cipher"),
            Map.of("secret-key", options(true, null))
        );

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);
        DictionaryEntity entity = DictionaryEntity.builder()
            .id("dic-1")
            .name("My dic")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .properties(
                dictionary
                    .getProperties()
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(DictionaryProperty::getKey, DictionaryProperty::getValue))
            )
            .propertyOptions(
                Map.of(
                    "secret-key",
                    io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions.builder().encrypted(true).build()
                )
            )
            .build();

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(entity, EXECUTION_CONTEXT);

        assertThat(state.getManual().getProperties()).containsExactlyInAnyOrderEntriesOf(spec.getManual().getProperties());
        assertThat(state.getManual().getPropertyOptions()).isEqualTo(spec.getManual().getPropertyOptions());
    }

    @Test
    void should_map_manual_dictionary_without_properties() {
        ManualDictionarySpec manual = new ManualDictionarySpec();
        manual.setProperties(null);
        DictionarySpec spec = new DictionarySpec().type(DictionaryType.MANUAL).manual(manual);

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).isNull();
    }

    private static DictionarySpec manualSpec(Map<String, String> properties, Map<String, DictionaryPropertyOptions> options) {
        ManualDictionarySpec manual = new ManualDictionarySpec();
        manual.setProperties(properties);
        manual.setPropertyOptions(options);
        return new DictionarySpec().type(DictionaryType.MANUAL).manual(manual);
    }

    private static DictionaryPropertyOptions options(Boolean encrypted, Boolean encryptable) {
        return new DictionaryPropertyOptions().encrypted(encrypted).encryptable(encryptable);
    }
}
