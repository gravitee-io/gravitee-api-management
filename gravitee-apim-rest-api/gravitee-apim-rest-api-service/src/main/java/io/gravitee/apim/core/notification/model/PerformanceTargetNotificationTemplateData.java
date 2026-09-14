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
package io.gravitee.apim.core.notification.model;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * What a performance target notification says, as the templates read it: the subject in the host's words (an API, an
 * agent), when the verdicts changed, and each rule that changed with its observed value against its threshold. One
 * instance carries every rule of one subject that changed the same way in one evaluation run.
 *
 * <p>Values come formatted ({@code observedText}, {@code thresholdText}, {@code windowText}) beside their raw
 * numbers, so a template can print them without knowing the metric's unit.
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class PerformanceTargetNotificationTemplateData {

    /** {@code API} or {@code Agent}, in the words a sentence can hold: "the agent Support Triage is missing...". */
    private final String subjectKind;

    private final String subjectName;
    private final String subjectReference;

    /** The subject's Targets page, when the console URL is known. */
    private final String subjectUrl;

    private final Date changedAt;
    private final String changedAtText;
    private final List<RuleChange> rules;

    @Getter
    @Builder(toBuilder = true)
    @AllArgsConstructor
    public static class RuleChange {

        private final String targetId;
        private final String ruleId;
        private final String metric;
        private final String metricLabel;
        private final String measure;
        private final String operator;

        /** The rule in words: {@code Gateway response time P95 ≤ 2000 ms}. */
        private final String description;

        private final double threshold;
        private final String thresholdText;

        /** Absent when the rule could not be evaluated. */
        private final Double observed;

        private final String observedText;
        private final String unit;
        private final long sampleCount;
        private final String status;
        private final String windowText;
        private final String intervalText;
    }
}
