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
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import java.util.List;
import java.util.Optional;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ParameterRepository extends FindAllRepository<Parameter> {
    Optional<Parameter> findById(String key, String referenceId, ParameterReferenceType referenceType) throws TechnicalException;

    Parameter create(Parameter item) throws TechnicalException;

    Parameter update(Parameter item) throws TechnicalException;

    void delete(String key, String referenceId, ParameterReferenceType referenceType) throws TechnicalException;

    List<Parameter> findByKeys(List<String> keys, String referenceId, ParameterReferenceType referenceType) throws TechnicalException;

    List<Parameter> findAll(String referenceId, ParameterReferenceType referenceType) throws TechnicalException;

    /**
     * Delete parameter by reference
     *
     * @param referenceId   The parameter reference id
     * @param referenceType The parameter reference type
     * @return List of keys for deleted parameter
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, ParameterReferenceType referenceType) throws TechnicalException;
}
