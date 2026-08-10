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
package io.gravitee.gamma.rest.core.observability.dashboard.exception;

import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.VersionPrecondition;
import lombok.Getter;

/**
 * The dashboard changed since the caller read it, so the write was refused rather than applied over someone else's
 * work.
 *
 * <p>Carries the whole current dashboard, not just its version: the client has to show the author what they are about
 * to overwrite, and making it fetch that separately would race with the next editor and give a diff against a third
 * state.
 *
 * @author GraviteeSource Team
 */
@Getter
public class DashboardVersionConflictException extends ConflictDomainException {

    private final transient Dashboard current;

    public DashboardVersionConflictException(Dashboard current, VersionPrecondition precondition) {
        super(
            "Dashboard '%s' has been modified since you loaded it (you required %s, the current version is %s)".formatted(
                current.id(),
                describe(precondition),
                current.version()
            ),
            current.id()
        );
        this.current = current;
    }

    /**
     * An overwrite reaches here only by losing to a concurrent delete-and-recreate, so say that rather than report a
     * version the caller never claimed.
     */
    private static String describe(VersionPrecondition precondition) {
        return precondition instanceof VersionPrecondition.OneOf oneOf ? "version " + oneOf.versions() : "any current revision";
    }
}
