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
package io.gravitee.apim.infra.domain_service.dictionary;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.apim.core.dictionary.model.DictionaryProperty;
import io.gravitee.apim.core.dictionary.model.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class DictionaryAutomationDomainServiceLegacyWrapperTest {

    @InjectMocks
    private DictionaryAutomationDomainServiceLegacyWrapper wrapper;

    @Mock
    private DictionaryService dictionaryService;

    private static List<DictionaryProperty> mixedProperties() {
        return List.of(
            new DictionaryProperty("plain-key", "plain-value", null, null),
            new DictionaryProperty("secret-key", "cipher", true, null)
        );
    }

    private static final Map<String, String> EXPECTED_PROPERTIES = Map.of("plain-key", "plain-value", "secret-key", "cipher");
    private static final Map<String, DictionaryPropertyOptions> EXPECTED_OPTIONS = Map.of(
        "secret-key",
        DictionaryPropertyOptions.builder().encrypted(true).build()
    );

    @Test
    void should_pass_encrypted_flag_to_the_create_entity() {
        Dictionary dictionary = Dictionary.builder()
            .name("My Dictionary")
            .type(DictionaryType.MANUAL)
            .properties(mixedProperties())
            .build();

        wrapper.create(GraviteeContext.getExecutionContext(), dictionary);

        verify(dictionaryService).create(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                (NewDictionaryEntity entity) ->
                    entity.getProperties().equals(EXPECTED_PROPERTIES) && entity.getPropertyOptions().equals(EXPECTED_OPTIONS)
            )
        );
    }

    @Test
    void should_skip_null_property_when_creating_a_legacy_dictionary() {
        List<DictionaryProperty> properties = new ArrayList<>(mixedProperties());
        properties.add(null);
        Dictionary dictionary = Dictionary.builder().name("My Dictionary").type(DictionaryType.MANUAL).properties(properties).build();

        wrapper.create(GraviteeContext.getExecutionContext(), dictionary);

        verify(dictionaryService).create(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                (NewDictionaryEntity entity) ->
                    entity.getProperties().equals(EXPECTED_PROPERTIES) && entity.getPropertyOptions().equals(EXPECTED_OPTIONS)
            )
        );
    }

    @Test
    void should_pass_encrypted_flag_to_the_update_entity() {
        Dictionary dictionary = Dictionary.builder()
            .name("My Dictionary")
            .type(DictionaryType.MANUAL)
            .properties(mixedProperties())
            .build();

        wrapper.update(GraviteeContext.getExecutionContext(), "dic-1", dictionary);

        verify(dictionaryService).update(
            eq(GraviteeContext.getExecutionContext()),
            eq("dic-1"),
            argThat(
                (UpdateDictionaryEntity entity) ->
                    entity.getProperties().equals(EXPECTED_PROPERTIES) && entity.getPropertyOptions().equals(EXPECTED_OPTIONS)
            )
        );
    }
}
