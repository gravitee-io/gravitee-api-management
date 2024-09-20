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
package io.gravitee.apim.core.scoring.domain_service;

import io.gravitee.apim.core.DomainService;
import java.math.BigDecimal;
import java.math.RoundingMode;

@DomainService
public class ScoreComputingDomainService {

    public Double computeScore(long nbErrors, long nbWarnings, long nbInfos, long nbHints) {
        var errorsWeight = 1.0;
        var warningsWeight = 0.5;
        var infosWeight = 0.2;
        var hintsWeight = 0.1;
        var aggressivenessParam = 0.1;

        var sum = ((errorsWeight * nbErrors) + (warningsWeight * nbWarnings) + (infosWeight * nbInfos) + (hintsWeight * nbHints));
        var agg = sum * aggressivenessParam;
        var exp = StrictMath.exp(-agg);

        return BigDecimal.valueOf(1).multiply(BigDecimal.valueOf(exp)).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
