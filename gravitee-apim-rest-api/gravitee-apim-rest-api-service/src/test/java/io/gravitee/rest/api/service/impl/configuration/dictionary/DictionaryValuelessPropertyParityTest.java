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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.dictionary.domain_service.ValidateDictionaryDomainService;
import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.apim.core.dictionary.model.DictionaryProperty;
import io.gravitee.apim.core.dictionary.model.DictionaryType;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * A valueless property is refused on two independent paths: the Automation API validates the core
 * model before the write, and the write itself guards again for callers that never pass through
 * core validation. The two cannot share an exception — core must not depend on this package — so
 * this test is what keeps the operator-facing message identical on both.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DictionaryValuelessPropertyParityTest {

    private static final String PROPERTY_KEY = "hostname";

    private final ValidateDictionaryDomainService validateDictionary = new ValidateDictionaryDomainService();

    @Test
    void should_report_the_same_message_on_both_paths() {
        Dictionary dictionary = Dictionary.builder()
            .type(DictionaryType.MANUAL)
            .properties(List.of(new DictionaryProperty(PROPERTY_KEY, null, null, null)))
            .build();

        assertThatThrownBy(() -> validateDictionary.validate(dictionary)).hasMessage(
            new DictionaryPropertyValueRequiredException(PROPERTY_KEY).getMessage()
        );
    }
}
