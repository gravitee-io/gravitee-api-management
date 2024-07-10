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

package io.gravitee.apim.infra.domain_service.score;

import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.score.domain_service.ScoreDomainService;
import io.gravitee.apim.core.score.model.ScoreResult;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestCommand;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestReply;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.scoring.api.model.ScoringRequest;
import io.gravitee.scoring.api.model.ScoringResult;
import io.gravitee.scoring.api.model.asset.AssetType;
import io.gravitee.scoring.api.model.asset.ScoreAsset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class ScoreDomainServiceImpl implements ScoreDomainService {

    private CockpitConnector cockpitConnector;

    private InstallationService installationService;

    private ApiDocumentationDomainService apiDocumentationDomainService;

    public ScoreDomainServiceImpl(
        @Lazy CockpitConnector cockpitConnector,
        InstallationService installationService,
        ApiDocumentationDomainService apiDocumentationDomainService
    ) {
        this.cockpitConnector = cockpitConnector;
        this.installationService = installationService;
        this.apiDocumentationDomainService = apiDocumentationDomainService;
    }

    @Override
    public List<ScoreResult> requestScore(String apiId, String envId, String orgId) {
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
                return new ScoreAsset(assetType, page.getContent());
            })
            .toList();

        ScoringRequest scoringRequest = new ScoringRequest(assets);
        ScoringRequestCommand command = new ScoringRequestCommand(
            apiId,
            envId,
            orgId,
            installationService.get().getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_ID),
            scoringRequest
        );

        var reply = cockpitConnector
            .sendCommand(command)
            .onErrorReturn(error ->
                new ScoringRequestReply(command.getId(), error.getMessage() != null ? error.getMessage() : error.toString())
            )
            .cast(ScoringRequestReply.class)
            .blockingGet();

        return List.of(new ScoreResult(reply.toString()));
    }

    @Override
    public void processResponse(String apiId, String envId, String orgId, ScoringResult scoreResult) {
        //Persist results
        log.info("received response [{}]", scoreResult);
        //Notify user ?
    }
}
