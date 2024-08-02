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
import io.gravitee.repository.management.model.Workflow;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface WorkflowRepository extends CrudRepository<Workflow, String> {
    List<Workflow> findByReferenceAndType(String referenceType, String referenceId, String type) throws TechnicalException;

    /**
     * Delete workflows by reference
     * @param referenceId
     * @param referenceType
     * @return List of IDs for deleted workflows
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, String referenceType) throws TechnicalException;
}
