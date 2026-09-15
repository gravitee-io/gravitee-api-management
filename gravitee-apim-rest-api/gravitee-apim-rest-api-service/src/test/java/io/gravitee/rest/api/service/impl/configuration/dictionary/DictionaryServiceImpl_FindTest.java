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
package io.gravitee.rest.api.service.impl.configuration.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceImpl_FindTest {

    @InjectMocks
    private DictionaryServiceImpl dictionaryService = new DictionaryServiceImpl();

    @Mock
    private DictionaryRepository dictionaryRepository;

    @Test
    void should_skip_null_property_when_finding_dictionary() throws TechnicalException {
        Dictionary stored = new Dictionary();
        stored.setId("dictionary-id");
        stored.setName("Dictionary");
        stored.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        stored.setType(DictionaryType.MANUAL);
        stored.setState(LifecycleState.STOPPED);
        Map<String, DictionaryProperty> properties = new HashMap<>();
        properties.put("valid", new DictionaryProperty("value", false));
        properties.put("invalid", null);
        stored.setProperties(properties);
        when(dictionaryRepository.findById("dictionary-id")).thenReturn(Optional.of(stored));

        DictionaryEntity result = dictionaryService.findById(GraviteeContext.getExecutionContext(), "dictionary-id");

        assertThat(result.getProperties()).containsExactlyEntriesOf(Map.of("valid", "value"));
    }

    @Test
    void should_keep_property_with_null_value_when_finding_dictionary() throws TechnicalException {
        Dictionary stored = new Dictionary();
        stored.setId("dictionary-id");
        stored.setName("Dictionary");
        stored.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        stored.setType(DictionaryType.MANUAL);
        stored.setState(LifecycleState.STOPPED);
        Map<String, DictionaryProperty> properties = new HashMap<>();
        properties.put("valued", new DictionaryProperty("value", false));
        properties.put("valueless", new DictionaryProperty(null, false));
        stored.setProperties(properties);
        when(dictionaryRepository.findById("dictionary-id")).thenReturn(Optional.of(stored));

        DictionaryEntity result = dictionaryService.findById(GraviteeContext.getExecutionContext(), "dictionary-id");

        Map<String, String> expected = new HashMap<>();
        expected.put("valued", "value");
        expected.put("valueless", null);
        assertThat(result.getProperties()).containsExactlyInAnyOrderEntriesOf(expected);
    }
}
