/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management.api;

import java.util.List;
import java.util.Optional;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ParameterRepository {
    
    Optional<Parameter> findById(String key) throws TechnicalException;

    Parameter create(Parameter item) throws TechnicalException;

    Parameter update(Parameter item) throws TechnicalException;

    void delete(String key) throws TechnicalException;
    
    List<Parameter> findAll(List<String> keys) throws TechnicalException;
    
    List<Parameter> findAllByReferenceIdAndReferenceType(List<String> keys, String referenceId, ParameterReferenceType referenceType) throws TechnicalException;
    
}
