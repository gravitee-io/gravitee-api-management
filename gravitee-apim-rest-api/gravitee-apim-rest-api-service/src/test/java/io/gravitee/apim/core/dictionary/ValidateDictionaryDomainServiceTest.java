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
package io.gravitee.apim.core.dictionary;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.dictionary.domain_service.ValidateDictionaryDomainService;
import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.apim.core.dictionary.model.DictionaryProvider;
import io.gravitee.apim.core.dictionary.model.DictionaryTrigger;
import io.gravitee.apim.core.dictionary.model.DictionaryType;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateDictionaryDomainServiceTest {

    private final ValidateDictionaryDomainService service = new ValidateDictionaryDomainService();

    @Nested
    class MutualExclusion {

        @Test
        void should_reject_when_both_provider_trigger_and_properties_are_set() {
            var dictionary = Dictionary.builder()
                .type(DictionaryType.DYNAMIC)
                .provider(new DictionaryProvider())
                .trigger(aTrigger())
                .properties(Map.of("key1", "v1", "key2", "v2"))
                .build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must not have 'manual' properties");
        }

        @Test
        void should_reject_when_both_properties_and_provider_trigger_are_set() {
            var dictionary = Dictionary.builder()
                .type(DictionaryType.MANUAL)
                .properties(Map.of("key", "value"))
                .provider(new DictionaryProvider())
                .trigger(aTrigger())
                .build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must not have 'dynamic' properties");
        }
    }

    @Nested
    class ManualDictionary {

        @Test
        void should_reject_with_null_properties() {
            var dictionary = Dictionary.builder().type(DictionaryType.MANUAL).build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("at least one property");
        }

        @Test
        void should_reject_with_empty_properties() {
            var dictionary = Dictionary.builder().type(DictionaryType.MANUAL).properties(Map.of()).build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("at least one property");
        }

        @Test
        void should_reject_when_provider_is_set() {
            var dictionary = Dictionary.builder()
                .type(DictionaryType.MANUAL)
                .properties(Map.of("key", "value"))
                .provider(new DictionaryProvider())
                .build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must not have 'dynamic' properties");
        }

        @Test
        void should_reject_when_trigger_is_set() {
            var dictionary = Dictionary.builder()
                .type(DictionaryType.MANUAL)
                .properties(Map.of("key", "value"))
                .trigger(new DictionaryTrigger())
                .build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must not have 'dynamic' properties");
        }

        @Test
        void should_accept_with_properties_only() {
            var dictionary = Dictionary.builder().type(DictionaryType.MANUAL).properties(Map.of("key", "value")).build();

            assertThatNoException().isThrownBy(() -> service.validate(dictionary));
        }
    }

    @Nested
    class DynamicDictionary {

        @Test
        void should_reject_without_provider() {
            var dictionary = Dictionary.builder().type(DictionaryType.DYNAMIC).trigger(aTrigger()).build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must have a provider and a trigger");
        }

        @Test
        void should_reject_without_trigger() {
            var dictionary = Dictionary.builder().type(DictionaryType.DYNAMIC).provider(new DictionaryProvider()).build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must have a provider and a trigger");
        }

        @Test
        void should_reject_when_properties_are_set() {
            var dictionary = Dictionary.builder()
                .type(DictionaryType.DYNAMIC)
                .provider(new DictionaryProvider())
                .trigger(aTrigger())
                .properties(Map.of("key", "value"))
                .build();

            assertThatThrownBy(() -> service.validate(dictionary))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must not have 'manual' properties");
        }

        @Test
        void should_accept_with_provider_and_trigger() {
            var dictionary = Dictionary.builder()
                .type(DictionaryType.DYNAMIC)
                .provider(new DictionaryProvider())
                .trigger(aTrigger())
                .build();

            assertThatNoException().isThrownBy(() -> service.validate(dictionary));
        }
    }

    private DictionaryTrigger aTrigger() {
        return DictionaryTrigger.builder().rate(5L).unit(TimeUnit.SECONDS).build();
    }
}
