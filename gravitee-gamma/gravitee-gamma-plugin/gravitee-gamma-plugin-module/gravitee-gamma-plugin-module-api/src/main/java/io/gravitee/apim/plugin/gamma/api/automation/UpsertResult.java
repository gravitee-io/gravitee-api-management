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
package io.gravitee.apim.plugin.gamma.api.automation;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of a validate or upsert call on a {@link GammaAutomationPort}.
 *
 * <p>{@code view} is never null: on a successful apply it is the persisted state; on a dry run, or when
 * severe issues prevented the apply, it is the module's redacted preview of the submitted spec. The
 * Automation API stamps the envelope fields onto it before answering.
 *
 * @param view the state, or its redacted preview
 * @param issues findings; a null list is read as no issues
 */
public record UpsertResult<V>(V view, List<AutomationIssue> issues) {
    public UpsertResult {
        Objects.requireNonNull(view, "view");
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public static <V> UpsertResult<V> of(V view) {
        return new UpsertResult<>(view, List.of());
    }

    public boolean hasSevere() {
        return issues.stream().anyMatch(AutomationIssue::isSevere);
    }
}
