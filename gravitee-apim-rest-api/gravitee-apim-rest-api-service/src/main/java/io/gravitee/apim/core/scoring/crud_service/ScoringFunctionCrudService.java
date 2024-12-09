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
package io.gravitee.apim.core.scoring.crud_service;

import io.gravitee.apim.core.scoring.model.ScoringFunction;
import java.util.Optional;

public interface ScoringFunctionCrudService {
    ScoringFunction create(ScoringFunction function);
    Optional<ScoringFunction> findById(String id);
    void delete(String id);
    void deleteByReference(String referenceId, ScoringFunction.ReferenceType referenceType);
    void deleteByReferenceAndName(String referenceId, ScoringFunction.ReferenceType referenceType, String name);
}