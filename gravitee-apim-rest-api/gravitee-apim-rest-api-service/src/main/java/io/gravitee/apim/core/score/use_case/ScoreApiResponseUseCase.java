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

package io.gravitee.apim.core.score.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.score.domain_service.ScoreDomainService;
import io.gravitee.scoring.api.model.ScoringResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
@Slf4j
public class ScoreApiResponseUseCase {

    private final ScoreDomainService scoreDomainService;

    public Output execute(Input input) {
        log.info("Received {}", input);

        scoreDomainService.processResponse(input.apiId(), input.envId(), input.orgId(), input.result());

        return new Output();
    }

    public record Input(String apiId, String envId, String orgId, ScoringResult result) {}

    public record Output() {}
}
