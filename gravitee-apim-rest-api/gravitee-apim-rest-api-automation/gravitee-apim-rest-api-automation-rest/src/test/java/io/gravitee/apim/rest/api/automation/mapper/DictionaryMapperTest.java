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

import io.gravitee.apim.rest.api.automation.model.DictionaryPropertyValue;
import io.gravitee.apim.rest.api.automation.model.DictionaryPropertyValueOneOf;
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
    void should_map_legacy_bare_string_property_to_state() {
        DictionaryEntity entity = DictionaryEntity.builder()
            .id("dic-1")
            .name("My dic")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .properties(Map.of("legacy-key", "legacy-value"))
            .build();

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(entity, EXECUTION_CONTEXT);

        DictionaryPropertyValue value = state.getManual().getProperties().get("legacy-key");
        assertThat(value.getDictionaryPropertyValueOneOf().getValue()).isEqualTo("legacy-value");
        assertThat(value.getDictionaryPropertyValueOneOf().getEncrypted()).isFalse();
    }

    @Test
    void should_map_dictionary_with_legacy_bare_string_property_to_core_dictionary() {
        DictionarySpec spec = new DictionarySpec()
            .type(DictionaryType.MANUAL)
            .manual(new ManualDictionarySpec().properties(Map.of("legacy-key", new DictionaryPropertyValue("legacy-value"))));

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsEntry("legacy-key", "legacy-value");
    }

    @Test
    void should_map_dictionary_with_typed_property_to_core_dictionary() {
        DictionarySpec spec = new DictionarySpec()
            .type(DictionaryType.MANUAL)
            .manual(
                new ManualDictionarySpec().properties(
                    Map.of("typed-key", new DictionaryPropertyValue(new DictionaryPropertyValueOneOf(true).value("cipher")))
                )
            );

        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);

        assertThat(dictionary.getProperties()).containsEntry("typed-key", "cipher");
    }
}
