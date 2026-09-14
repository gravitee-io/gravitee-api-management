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
package inmemory;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.core.performance_target.service_provider.PerformanceTargetTransitionPublisher;
import java.util.ArrayList;
import java.util.List;

/** Keeps every run published, so tests can tell how many runs were published and what each carried. */
public class PerformanceTargetTransitionPublisherInMemory implements PerformanceTargetTransitionPublisher {

    private final List<List<PerformanceTargetRuleTransition>> runs = new ArrayList<>();

    @Override
    public void publish(List<PerformanceTargetRuleTransition> transitions) {
        runs.add(List.copyOf(transitions));
    }

    public List<List<PerformanceTargetRuleTransition>> runs() {
        return runs;
    }

    public List<PerformanceTargetRuleTransition> published() {
        return runs.stream().flatMap(List::stream).toList();
    }

    public void reset() {
        runs.clear();
    }
}
