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
package io.gravitee.apim.core.portal.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.List;

public class SeveralPortalsInEnvironmentException extends ValidationDomainException {

    /** @param portals each portal of the environment, described by its id and name */
    public SeveralPortalsInEnvironmentException(String environmentId, List<String> portals) {
        super(
            "Environment [%s] has %d portals (%s) but can have only one: keep the portal to use and remove the other portal records of this environment from the portals table or collection".formatted(
                environmentId,
                portals.size(),
                String.join(", ", portals)
            )
        );
    }
}
