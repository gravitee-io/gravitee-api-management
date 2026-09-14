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
package io.gravitee.apim.core.performance_target.model;

/**
 * When a change of verdict is worth telling the owners about.
 *
 * @param notEvaluableAfter consecutive not-evaluable windows before a rule that could be evaluated is reported as no
 *                          longer evaluable; one empty window is not news
 */
public record PerformanceTargetNotificationPolicy(int notEvaluableAfter) {
    public static final PerformanceTargetNotificationPolicy DEFAULT = new PerformanceTargetNotificationPolicy(3);

    public PerformanceTargetNotificationPolicy {
        if (notEvaluableAfter < 1) {
            throw new IllegalArgumentException("A rule is reported not evaluable after at least 1 window");
        }
    }
}
