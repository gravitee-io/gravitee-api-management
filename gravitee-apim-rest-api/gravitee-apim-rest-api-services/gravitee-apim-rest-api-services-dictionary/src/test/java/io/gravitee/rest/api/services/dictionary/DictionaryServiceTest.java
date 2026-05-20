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
package io.gravitee.rest.api.services.dictionary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.node.api.Node;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.event.DictionaryEvent;
import io.vertx.core.Vertx;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock
    private Vertx vertx;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private io.gravitee.rest.api.service.configuration.dictionary.DictionaryService dictionaryManagementService;

    @Mock
    private Node node;

    @InjectMocks
    private DictionaryService dictionaryService;

    @Test
    void should_limit_dictionary_polling_delay_when_configured_delay_is_slower() {
        ReflectionTestUtils.setField(dictionaryService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(dictionaryService, "delayLimitMillis", 60_000L);
        ReflectionTestUtils.setField(dictionaryService, "dictionaryService", dictionaryManagementService);

        dictionaryService.onEvent(new SimpleEvent<>(DictionaryEvent.START, dictionary()));

        verify(vertx).setPeriodic(eq(60_000L), any(DictionaryRefresher.class));
    }

    private DictionaryEntity dictionary() {
        var provider = new DictionaryProviderEntity();
        provider.setType("HTTP");
        provider.setConfiguration(
            new ObjectMapper()
                .createObjectNode()
                .put("url", "not-a-url")
                .put(
                    "specification",
                    "[{\"operation\":\"shift\",\"spec\":{\"content\":{\"*\":{\"$\":\"[#2].key\",\"@\":\"[#2].value\"}}}}]"
                )
        );

        var trigger = new DictionaryTriggerEntity();
        trigger.setRate(1);
        trigger.setUnit(TimeUnit.SECONDS);

        return DictionaryEntity.builder().id("dictionary-id").name("dictionary").provider(provider).trigger(trigger).build();
    }
}
