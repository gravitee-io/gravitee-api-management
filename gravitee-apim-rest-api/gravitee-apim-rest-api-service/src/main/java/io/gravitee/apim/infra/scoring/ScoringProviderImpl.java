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
package io.gravitee.apim.infra.scoring;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.scoring.model.ScoreRequest;
import io.gravitee.apim.core.scoring.service_provider.ScoringProvider;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestCommand;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestCommandPayload;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.scoring.api.model.ScoringRequest;
import io.gravitee.scoring.api.model.asset.AssetToAnalyze;
import io.gravitee.scoring.api.model.asset.AssetType;
import io.gravitee.scoring.api.model.asset.ContentType;
import io.reactivex.rxjava3.core.Completable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScoringProviderImpl implements ScoringProvider {

    private final CockpitConnector cockpitConnector;
    private final InstallationService installationService;

    public ScoringProviderImpl(@Lazy CockpitConnector cockpitConnector, InstallationService installationService) {
        this.cockpitConnector = cockpitConnector;
        this.installationService = installationService;
    }

    @Override
    public Completable requestScore(ScoreRequest request) {
        ScoringRequestCommand command = new ScoringRequestCommand(
            new ScoringRequestCommandPayload(
                request.jobId(),
                request.organizationId(),
                request.environmentId(),
                installationService.get().getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_ID),
                new ScoringRequest(
                    request
                        .assets()
                        .stream()
                        .map(a ->
                            new AssetToAnalyze(
                                a.assetId(),
                                switch (a.assetType()) {
                                    case SWAGGER -> AssetType.OPEN_API;
                                    case ASYNCAPI -> AssetType.ASYNC_API;
                                    case GRAVITEE_DEFINITION -> AssetType.GRAVITEE_API;
                                },
                                a.assetName(),
                                a.content(),
                                detectContentType(a.content())
                            )
                        )
                        .toList(),
                    request.customRulesets()
                )
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

    private ContentType detectContentType(String content) {
        if (content.startsWith("{")) {
            return ContentType.JSON;
        }
        return ContentType.YAML;
    }
}
