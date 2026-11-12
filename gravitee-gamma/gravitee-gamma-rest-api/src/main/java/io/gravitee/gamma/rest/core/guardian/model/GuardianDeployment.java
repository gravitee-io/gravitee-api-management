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
package io.gravitee.gamma.rest.core.guardian.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * The latest publication or withdrawal of a Guardian, which its current draft is compared with.
 *
 * <p>Its dates are kept at the millisecond, the precision the Guardian store keeps: the update date of a draft is
 * advanced to them, and a date read back from the store must equal the one first returned.
 */
public sealed interface GuardianDeployment {
    Instant at();

    /** The definition the gateways run since {@code at}. */
    record Published(io.gravitee.definition.model.v4.guardian.Guardian definition, Instant at) implements GuardianDeployment {
        public Published {
            at = at.truncatedTo(ChronoUnit.MILLIS);
        }
    }

    /**
     * Withdrawn from the gateways at {@code at}.
     *
     * @param deployedAt the date of the publication withdrawn; {@code null} when the withdrawal does not carry it.
     */
    record Withdrawn(Instant deployedAt, Instant at) implements GuardianDeployment {
        public Withdrawn {
            deployedAt = deployedAt == null ? null : deployedAt.truncatedTo(ChronoUnit.MILLIS);
            at = at.truncatedTo(ChronoUnit.MILLIS);
        }
    }
}
