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
package io.gravitee.apim.core.scoring.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.apim.core.scoring.query_service.ScoringRulesetQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetEnvironmentRulesetsUseCase {

    private final ScoringRulesetQueryService scoringRulesetQueryService;

    public Output execute(Input input) {
        var result = scoringRulesetQueryService.findByReference(input.environmentId(), ScoringRuleset.ReferenceType.ENVIRONMENT);
        return new Output(result);
    }

    public record Input(String environmentId) {}

    public record Output(List<ScoringRuleset> reports) {}
}
