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
package io.gravitee.repository.management.api;/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import java.util.Collection;
import java.util.List;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface FlowRepository extends CrudRepository<Flow, String> {
    List<Flow> findByReference(FlowReferenceType referenceType, String referenceId) throws TechnicalException;

    void deleteAllById(Collection<String> ids) throws TechnicalException;

    /**
     * Delete flows by reference
     * @param referenceId
     * @param referenceType
     * @return List of IDs for deleted flows
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, FlowReferenceType referenceType) throws TechnicalException;
}
