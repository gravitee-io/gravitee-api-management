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
package io.gravitee.gamma.rest.core.guardian.exception;

import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.gamma.rest.core.guardian.model.ApiReference;
import java.util.List;
import java.util.stream.Collectors;

public class GuardianInUseException extends ConflictDomainException {

    /** What an API running the Guardian refuses. The wording of each lives here, where the message is built. */
    public enum RefusedChange {
        DELETION("deleted"),
        UNDEPLOYMENT("undeployed"),
        PHASE_OR_API_TYPES_CHANGE("moved to another phase or to other API types"),
        PUBLICATION_ELSEWHERE("deployed on another phase or on other API types than the revision its APIs run");

        private final String wording;

        RefusedChange(String wording) {
            this.wording = wording;
        }
    }

    /**
     * @param named the APIs the message names, which may be fewer than all of them
     * @param total how many APIs run the Guardian
     */
    public GuardianInUseException(String guardianId, RefusedChange refusedChange, List<ApiReference> named, long total) {
        super(
            "Guardian '" +
                guardianId +
                "' cannot be " +
                refusedChange.wording +
                " while it is enabled on APIs: " +
                named.stream().map(ApiReference::name).collect(Collectors.joining(", ")) +
                (total > named.size() ? " and " + (total - named.size()) + " more." : ".")
        );
    }
}
