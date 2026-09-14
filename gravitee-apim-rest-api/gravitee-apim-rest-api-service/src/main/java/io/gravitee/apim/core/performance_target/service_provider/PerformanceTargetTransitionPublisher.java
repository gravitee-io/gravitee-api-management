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
package io.gravitee.apim.core.performance_target.service_provider;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import java.util.List;

/**
 * Hands the transitions of one evaluation run over to whoever tells the owners. Returns at once: delivery happens
 * off the caller's thread, so a notifier outage can neither stall nor fail an evaluation.
 */
public interface PerformanceTargetTransitionPublisher {
    void publish(List<PerformanceTargetRuleTransition> transitions);
}
