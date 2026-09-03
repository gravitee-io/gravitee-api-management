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
package io.gravitee.repository.mongodb.management.internal.model;

import io.gravitee.repository.management.model.PerformanceTargetEvaluation;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}performance_target_evaluations")
public class PerformanceTargetEvaluationMongo {

    @Id
    private String id;

    private String targetId;
    private String environmentId;
    private String reference;
    private PerformanceTargetEvaluation.Status status;
    private List<PerformanceTargetEvaluation.RuleResult> rules;
    private Date windowFrom;
    private Date windowTo;
    private List<String> coveredApiIds;
    private Date evaluatedAt;
    private boolean latest;
}
