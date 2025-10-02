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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
public interface PortalPageContextRepository extends CrudRepository<PortalPageContext, String> {
    /**
     * Find all portal page contexts by context type and environment ID.
     *
     * @param contextType the context type
     * @param environmentId the environment ID
     * @return a list of portal page contexts for the given context type and environment
     * @throws TechnicalException if an error occurs
     */
    List<PortalPageContext> findAllByContextTypeAndEnvironmentId(PortalPageContextType contextType, String environmentId)
        throws TechnicalException;

    PortalPageContext findByPageId(String string);

    PortalPageContext updateByPageId(PortalPageContext item) throws TechnicalException;

    void deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
