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

import java.util.Objects;

/**
 * A finding a module reports about an automation spec. Severe issues block the apply — nothing is
 * persisted; warnings are informational and do not block. The Automation API surfaces them in the
 * {@code errors} envelope of the returned state.
 */
public record AutomationIssue(Severity severity, String message) {
    public enum Severity {
        SEVERE,
        WARNING,
    }

    public AutomationIssue {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }

    public static AutomationIssue severe(String message) {
        return new AutomationIssue(Severity.SEVERE, message);
    }

    public static AutomationIssue warning(String message) {
        return new AutomationIssue(Severity.WARNING, message);
    }

    public boolean isSevere() {
        return severity == Severity.SEVERE;
    }
}
