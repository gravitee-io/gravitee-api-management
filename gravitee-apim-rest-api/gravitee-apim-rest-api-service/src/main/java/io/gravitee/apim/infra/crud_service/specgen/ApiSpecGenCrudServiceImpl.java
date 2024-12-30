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

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.specgen.crud_service.ApiSpecGenCrudService;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.service.ApiService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class ApiSpecGenCrudServiceImpl implements ApiSpecGenCrudService {

    private final ApiRepository apiRepository;
    private final ObjectMapper objectMapper;
    private final ApiService apiService;

    public ApiSpecGenCrudServiceImpl(@Lazy ApiRepository apiRepository, @Lazy ApiService apiService, ObjectMapper objectMapper) {
        this.apiRepository = apiRepository;
        this.objectMapper = objectMapper;
        this.apiService = apiService;
    }

    @Override
    public void enableAnalyticsLogging(ApiSpecGen apiSpecGen, String userId) {
        try {
            var apiDefinition = objectMapper.readValue(apiSpecGen.definition(), io.gravitee.definition.model.v4.Api.class);

            boolean deploy = false;

            var analytics = ofNullable(apiDefinition.getAnalytics()).orElse(getAnalytics());
            deploy = enableAnalytics(analytics, deploy);

            var logging = analytics.getLogging();
            deploy = enableEndointMode(logging, deploy);

            var content = logging.getContent();
            deploy = enableHeaders(content, deploy);
            deploy = enablePayload(content, deploy);

            var phase = logging.getPhase();
            deploy = enableRequest(phase, deploy);
            deploy = enableResponse(phase, deploy);

            if (deploy) {
                apiDefinition.setAnalytics(analytics);
                String definition = objectMapper.writeValueAsString(apiDefinition);

                apiRepository.findById(apiSpecGen.id()).ifPresent(api -> updateApi(api, definition, userId));
            }
        } catch (Exception e) {
            log.error("Could not enable analytics for api {}, reason: {}", apiSpecGen.id(), e.getMessage(), e);
        }
    }

    private void updateApi(io.gravitee.repository.management.model.Api api, String definition, String userId) {
        try {
            api.setDefinition(definition);
            api = apiRepository.update(api);
            deploy(api, userId);
        } catch (TechnicalException technicalException) {
            throw new RuntimeException(technicalException);
        }
    }

    private void deploy(Api api, String userId) {
        Completable
            .defer(() ->
                Completable.fromRunnable(() -> {
                    var apiDeploymentEntity = new ApiDeploymentEntity();
                    apiDeploymentEntity.setDeploymentLabel("Analytics enabled by spec-gen");
                    apiService.deploy(getExecutionContext(), api.getId(), userId, PUBLISH_API, apiDeploymentEntity);
                })
            )
            .subscribeOn(Schedulers.io())
            .subscribe(
                () -> log.debug("Api [{}] successfully deployed by user [{}]", api.getId(), userId),
                t -> log.error("Could not deploy api [{}], userId: [{}]", api.getId(), userId, t)
            );
    }

    private static boolean enableResponse(LoggingPhase phase, boolean deploy) {
        if (!phase.isResponse()) {
            phase.setResponse(true);
            deploy = true;
        }
        return deploy;
    }

    private static boolean enableRequest(LoggingPhase phase, boolean deploy) {
        if (!phase.isRequest()) {
            phase.setRequest(true);
            deploy = true;
        }
        return deploy;
    }

    private static boolean enablePayload(LoggingContent content, boolean deploy) {
        if (!content.isPayload()) {
            content.setPayload(true);
            deploy = true;
        }
        return deploy;
    }

    private static boolean enableHeaders(LoggingContent content, boolean deploy) {
        if (!content.isHeaders()) {
            content.setHeaders(true);
            deploy = true;
        }
        return deploy;
    }

    private static boolean enableEndointMode(Logging logging, boolean deploy) {
        if (!logging.getMode().isEndpoint()) {
            logging.getMode().setEndpoint(true);
            deploy = true;
        }
        return deploy;
    }

    private static boolean enableAnalytics(Analytics analytics, boolean deploy) {
        if (!analytics.isEnabled()) {
            analytics.setEnabled(true);
            deploy = true;
        }
        return deploy;
    }

    @NotNull
    private static Analytics getAnalytics() {
        return Analytics.builder().logging(getLogging()).build();
    }

    @NotNull
    private static Logging getLogging() {
        var logging = new Logging();
        logging.setMode(LoggingMode.builder().build());
        logging.setContent(LoggingContent.builder().build());
        logging.setPhase(LoggingPhase.builder().build());
        return logging;
    }
}
