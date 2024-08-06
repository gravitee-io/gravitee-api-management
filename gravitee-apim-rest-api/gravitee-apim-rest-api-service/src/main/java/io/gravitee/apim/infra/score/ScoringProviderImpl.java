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
package io.gravitee.apim.infra.score;

import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.score.service_provider.ScoringProvider;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestCommand;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestCommandPayload;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.scoring.api.model.ScoringRequest;
import io.gravitee.scoring.api.model.asset.AssetType;
import io.gravitee.scoring.api.model.asset.ContentType;
import io.gravitee.scoring.api.model.asset.ScoreAsset;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScoringProviderImpl implements ScoringProvider {

    private final CockpitConnector cockpitConnector;
    private final InstallationService installationService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;

    public ScoringProviderImpl(
        @Lazy CockpitConnector cockpitConnector,
        InstallationService installationService,
        ApiDocumentationDomainService apiDocumentationDomainService
    ) {
        this.cockpitConnector = cockpitConnector;
        this.installationService = installationService;
        this.apiDocumentationDomainService = apiDocumentationDomainService;
    }

    @Override
    public Completable requestScore(String apiId, String organizationId, String environmentId) {
        //Get OAS or Async API
        List<Page> pages = apiDocumentationDomainService.getApiPages(apiId, null);
        List<ScoreAsset> assets = pages
            .stream()
            .filter(page -> page.isAsyncApi() || page.isSwagger())
            .map(page -> {
                var assetType =
                    switch (page.getType()) {
                        case SWAGGER -> AssetType.OPEN_API;
                        case ASYNCAPI -> AssetType.ASYNC_API;
                        default -> throw new IllegalStateException("Unexpected value: " + page.getType());
                    };
                return new ScoreAsset(assetType, page.getName(), page.getContent(), extractContentType(page.getName()));
            })
            .toList();

        ScoringRequestCommand command = new ScoringRequestCommand(
            new ScoringRequestCommandPayload(
                apiId,
                environmentId,
                organizationId,
                installationService.get().getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_ID),
                new ScoringRequest(assets)
            )
        );

        return cockpitConnector
            .sendCommand(command)
            .onErrorReturn(error ->
                new ScoringRequestReply(command.getId(), error.getMessage() != null ? error.getMessage() : error.toString())
            )
            .cast(ScoringRequestReply.class)
            .flatMapCompletable(reply -> {
                if (reply.getCommandStatus() == CommandStatus.ERROR) {
                    return Completable.error(new TechnicalDomainException(reply.getErrorDetails()));
                }
                return Completable.complete();
            });
    }

    private ContentType extractContentType(String pageName) {
        if (pageName.endsWith(".json")) {
            return ContentType.JSON;
        } else if (pageName.endsWith(".yaml") || pageName.endsWith(".yml")) {
            return ContentType.YAML;
        }
        throw new IllegalArgumentException("Unsupported content type for page: " + pageName);
    }
}
