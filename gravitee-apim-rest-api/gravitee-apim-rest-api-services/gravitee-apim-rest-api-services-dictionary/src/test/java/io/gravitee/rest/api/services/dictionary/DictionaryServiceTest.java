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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.node.api.Node;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.service.EnvironmentService;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private io.gravitee.common.event.EventManager eventManager;

    @Mock
    private io.gravitee.rest.api.service.configuration.dictionary.DictionaryService dictionaryService;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Vertx vertx;

    @Mock
    private Node node;

    @InjectMocks
    private DictionaryService cut;

    @Test
    void should_call_the_endpoint_of_a_dynamic_dictionary_with_the_client_of_its_organization() throws Exception {
        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environment);
        when(vertx.setPeriodic(anyLong(), any())).thenReturn(1L);

        cut.onEvent(new SimpleEvent<>(DictionaryEvent.START, dynamicDictionary()));

        verify(httpClientService).createHttpClient(eq(ORGANIZATION_ID), eq("http"), any());
    }

    private DictionaryEntity dynamicDictionary() throws Exception {
        DictionaryProviderEntity provider = new DictionaryProviderEntity();
        provider.setType("HTTP");
        provider.setConfiguration(
            new ObjectMapper().readTree(
                "{\"url\":\"http://localhost:1/dictionary\",\"specification\":\"[{\\\"operation\\\":\\\"shift\\\",\\\"spec\\\":{\\\"*\\\":\\\"&\\\"}}]\"}"
            )
        );
        DictionaryTriggerEntity trigger = new DictionaryTriggerEntity();
        trigger.setRate(10);
        trigger.setUnit(TimeUnit.SECONDS);
        return DictionaryEntity.builder().id("dictionary-id").environmentId(ENVIRONMENT_ID).provider(provider).trigger(trigger).build();
    }
}
