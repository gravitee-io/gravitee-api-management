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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScoreComputingDomainServiceTest {

    private final ScoreComputingDomainService scoreComputingDomainService = new ScoreComputingDomainService();

    @ParameterizedTest
    @CsvSource(
        delimiterString = "|",
        textBlock = """
        # ERRORS | WARNINGS | INFOS | HINTS | EXPECTED_SCORE
          0      | 0        | 0     | 0     | 1
          0      | 0        | 0     | 1     | 0.99
          0      | 0        | 1     | 0     | 0.98
          0      | 1        | 0     | 0     | 0.95
          1      | 0        | 0     | 0     | 0.9
          0      | 1        | 0     | 1     | 0.94
          1      | 1        | 1     | 1     | 0.84
          1      | 2        | 3     | 4     | 0.74
          4      | 3        | 2     | 1     | 0.55
        """
    )
    void should_compute_score(Long nbErrors, Long nbWarnings, Long nbInfos, Long nbHints, Double expectedScore) {
        Assertions.assertThat(scoreComputingDomainService.computeScore(nbErrors, nbWarnings, nbInfos, nbHints)).isEqualTo(expectedScore);
    }
}
