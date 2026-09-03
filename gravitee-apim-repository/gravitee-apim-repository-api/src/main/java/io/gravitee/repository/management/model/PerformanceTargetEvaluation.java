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
package io.gravitee.repository.management.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@ToString
public final class PerformanceTargetEvaluation {

    @EqualsAndHashCode.Include
    private String id;

    private String targetId;
    private String environmentId;
    private String reference;
    private Status status;

    @Builder.Default
    private List<RuleResult> rules = Collections.emptyList();

    private Date windowFrom;
    private Date windowTo;

    @Builder.Default
    private List<String> coveredApiIds = Collections.emptyList();

    private Date evaluatedAt;
    private boolean latest;

    public enum Status {
        PASS,
        BREACH,
        NOT_EVALUABLE,
    }

    public record RuleResult(
        String metric,
        String measure,
        String operator,
        double threshold,
        Double observed,
        Double deviation,
        Double deviationRatio,
        long sampleCount,
        Status status
    ) {}
}
