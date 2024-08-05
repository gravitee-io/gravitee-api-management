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

import io.gravitee.apim.core.score.use_case.ScoreApiResponseUseCase;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.scoring.response.ScoringResponseCommand;
import io.gravitee.cockpit.api.command.v1.scoring.response.ScoringResponseReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.CommandStatus;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ScoringResponseCommandHandler implements CommandHandler<ScoringResponseCommand, ScoringResponseReply> {

    private final ScoreApiResponseUseCase scoreApiResponseUseCase;

    @Override
    public String supportType() {
        return CockpitCommandType.SCORING_RESPONSE.name();
    }

    @Override
    public Single<ScoringResponseReply> handle(ScoringResponseCommand command) {
        var payload = command.getPayload();

        scoreApiResponseUseCase.execute(
            new ScoreApiResponseUseCase.Input(payload.correlationId(), payload.envId(), payload.orgId(), payload.result())
        );

        return Single.just(new ScoringResponseReply(command.getId(), CommandStatus.SUCCEEDED));
    }
}
