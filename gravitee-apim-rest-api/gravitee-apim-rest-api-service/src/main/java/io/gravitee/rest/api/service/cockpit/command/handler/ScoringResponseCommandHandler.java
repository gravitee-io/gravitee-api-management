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

package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.use_case.SaveScoringResponseUseCase;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.scoring.response.ScoringResponseCommand;
import io.gravitee.cockpit.api.command.v1.scoring.response.ScoringResponseReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.CommandStatus;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScoringResponseCommandHandler implements CommandHandler<ScoringResponseCommand, ScoringResponseReply> {

    private SaveScoringResponseUseCase saveScoringResponseUseCase;

    public ScoringResponseCommandHandler(@Lazy SaveScoringResponseUseCase saveScoringResponseUseCase) {
        this.saveScoringResponseUseCase = saveScoringResponseUseCase;
    }

    @Override
    public String supportType() {
        return CockpitCommandType.SCORING_RESPONSE.name();
    }

    @Override
    public Single<ScoringResponseReply> handle(ScoringResponseCommand command) {
        var payload = command.getPayload();

        log.info("received response [{}]", payload.result());

        var analyzedAssets = payload
            .result()
            .assetDiagnostics()
            .stream()
            .map(a ->
                new ScoringReport.Asset(
                    a.asset().assetId(),
                    switch (a.asset().type()) {
                        case OPEN_API -> ScoringAssetType.SWAGGER;
                        case ASYNC_API -> ScoringAssetType.ASYNCAPI;
                        case GRAVITEE_API -> ScoringAssetType.GRAVITEE_DEFINITION;
                    },
                    a
                        .diagnostics()
                        .stream()
                        .map(d -> {
                            return new ScoringReport.Diagnostic(
                                ScoringReport.Severity.valueOf(d.severity().name()),
                                new ScoringReport.Range(
                                    new ScoringReport.Position(d.range().start().line(), d.range().start().character()),
                                    new ScoringReport.Position(d.range().end().line(), d.range().end().character())
                                ),
                                d.rule(),
                                d.message(),
                                d.path()
                            );
                        })
                        .toList()
                )
            )
            .toList();

        return saveScoringResponseUseCase
            .execute(new SaveScoringResponseUseCase.Input(payload.correlationId(), analyzedAssets))
            .andThen(Single.just(new ScoringResponseReply(command.getId(), CommandStatus.SUCCEEDED)));
    }
}
