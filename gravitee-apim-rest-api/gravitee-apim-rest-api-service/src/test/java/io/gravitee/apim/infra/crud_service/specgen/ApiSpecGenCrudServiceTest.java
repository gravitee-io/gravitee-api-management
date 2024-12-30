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
package io.gravitee.apim.infra.crud_service.specgen;

import static io.gravitee.definition.model.DefinitionVersion.V4;
import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */

@ExtendWith(MockitoExtension.class)
class ApiSpecGenCrudServiceTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    ApiService apiService;

    @Mock
    ApiRepository apiRepository;

    ApiSpecGenCrudServiceImpl crudService;

    @BeforeEach
    void setup() {
        crudService = new ApiSpecGenCrudServiceImpl(apiRepository, apiService, objectMapper);
        GraviteeContext.setCurrentEnvironment(generateRandom());
        GraviteeContext.setCurrentOrganization(generateRandom());
    }

    @AfterEach
    void cleanUp() {
        GraviteeContext.cleanContext();
    }

    @Test
    void must_not_deploy_due_to_already_activated_logging() throws TechnicalException, JsonProcessingException {
        var analytics = getCompleteAnalytics();

        var api = Api
            .builder()
            .id(generateRandom())
            .name("Api name")
            .definitionVersion(V4)
            .apiVersion("1.0.0")
            .analytics(analytics)
            .build();

        final ApiSpecGen apiSpecGen = new ApiSpecGen(
            api.getId(),
            api.getName(),
            api.getName(),
            api.getApiVersion(),
            ApiType.PROXY,
            getExecutionContext().getEnvironmentId(),
            objectMapper.writeValueAsString(api)
        );

        crudService.enableAnalyticsLogging(apiSpecGen, generateRandom());

        verify(apiRepository, times(0)).findById(api.getId());
        verify(apiRepository, times(0)).update(any());
        verify(apiService, times(0)).deploy(any(), any(), any(), any(), any());
    }

    @Test
    void must_deploy_due_to_already_activated_logging() throws TechnicalException {
        var api = Api.builder().id(generateRandom()).name("Api name").definitionVersion(V4).apiVersion("1.0.0").build();

        final ApiSpecGen apiSpecGen = new ApiSpecGen(
            api.getId(),
            api.getName(),
            api.getName(),
            api.getApiVersion(),
            ApiType.PROXY,
            getExecutionContext().getEnvironmentId(),
            "{}"
        );

        final String userId = generateRandom();
        var apiModel = new io.gravitee.repository.management.model.Api();
        apiModel.setId(api.getId());
        apiModel.setName(api.getName());

        when(apiRepository.findById(api.getId())).thenReturn(Optional.of(apiModel));
        when(apiRepository.update(apiModel)).thenReturn(apiModel);
        when(apiService.deploy(eq(getExecutionContext()), eq(apiSpecGen.id()), eq(userId), eq(PUBLISH_API), any()))
            .thenReturn(new ApiEntity());

        crudService.enableAnalyticsLogging(apiSpecGen, userId);

        verify(apiRepository, times(1)).findById(api.getId());
        verify(apiRepository, times(1)).update(any());

        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() -> {
                verify(apiService, times(1)).deploy(eq(getExecutionContext()), eq(apiSpecGen.id()), eq(userId), eq(PUBLISH_API), any());
                return true;
            });
    }

    private static Analytics getCompleteAnalytics() {
        var logging = new Logging();
        logging.setMode(LoggingMode.builder().endpoint(true).build());
        logging.setContent(LoggingContent.builder().headers(true).payload(true).build());
        logging.setPhase(LoggingPhase.builder().request(true).response(true).build());

        return Analytics.builder().enabled(true).logging(logging).build();
    }
}
