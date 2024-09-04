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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.scoring.model.ScoreRequest;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestCommand;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestCommandPayload;
import io.gravitee.cockpit.api.command.v1.scoring.request.ScoringRequestReply;
import io.gravitee.exchange.api.command.Command;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.scoring.api.model.ScoringRequest;
import io.gravitee.scoring.api.model.asset.AssetToAnalyze;
import io.gravitee.scoring.api.model.asset.AssetType;
import io.gravitee.scoring.api.model.asset.ContentType;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringProviderImplTest {

    private static final String API_ID = "api-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String INSTALLATION_ID = "installation-id";
    private static final String JOB_ID = "job-id";

    @Mock
    CockpitConnector controller;

    @Mock
    InstallationService installationService;

    ScoringProviderImpl scoringProvider;

    @BeforeEach
    void setUp() {
        scoringProvider = new ScoringProviderImpl(controller, installationService);

        lenient()
            .when(installationService.get())
            .thenReturn(
                InstallationEntity
                    .builder()
                    .additionalInformation(Map.of(InstallationService.COCKPIT_INSTALLATION_ID, INSTALLATION_ID))
                    .build()
            );
    }

    @Nested
    class RequestScore {

        @BeforeEach
        void setUp() {
            lenient()
                .when(controller.sendCommand(any()))
                .thenReturn(Single.just(new ScoringRequestReply("command-id", CommandStatus.SUCCEEDED)));
        }

        @Test
        void should_send_command_to_score_assets() {
            scoringProvider.requestScore(aRequest()).test().awaitDone(10, TimeUnit.SECONDS);

            var captor = ArgumentCaptor.forClass(Command.class);
            Mockito.verify(controller).sendCommand(captor.capture());

            assertThat(captor.getValue())
                .isInstanceOf(ScoringRequestCommand.class)
                .extracting(Command::getPayload)
                .isEqualTo(
                    new ScoringRequestCommandPayload(
                        JOB_ID,
                        ORGANIZATION_ID,
                        ENVIRONMENT_ID,
                        INSTALLATION_ID,
                        new ScoringRequest(
                            List.of(new AssetToAnalyze("page-id", AssetType.OPEN_API, "echo-oas.json", "{}", ContentType.JSON))
                        )
                    )
                );
        }

        @Test
        void should_throw_when_command_fails() {
            when(controller.sendCommand(any())).thenReturn(Single.just(new ScoringRequestReply("command-id", "Fail to score")));

            scoringProvider
                .requestScore(aRequest())
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(TechnicalDomainException.class).hasMessage("Fail to score");
                    return true;
                });
        }

        private @NotNull ScoreRequest aRequest() {
            return new ScoreRequest(
                JOB_ID,
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                API_ID,
                List.of(new ScoreRequest.AssetToScore("page-id", ScoringAssetType.SWAGGER, "echo-oas.json", "{}"))
            );
        }
    }
}
