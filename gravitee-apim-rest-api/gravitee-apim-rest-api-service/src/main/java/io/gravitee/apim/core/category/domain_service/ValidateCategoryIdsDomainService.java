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
package io.gravitee.apim.core.category.domain_service;

import static io.gravitee.apim.core.validation.Validator.Error.warning;
import static java.util.Optional.ofNullable;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.category.query_service.CategoryQueryService;
import io.gravitee.apim.core.validation.Validator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateCategoryIdsDomainService implements Validator<ValidateCategoryIdsDomainService.Input> {

    private static final String WARNING_FORMAT = "category [%s] is not defined in environment [%s]";

    public record Input(String environmentId, Set<String> idOrKeys) implements Validator.Input {
        Input sanitized(Collection<String> ids) {
            return new Input(environmentId, new HashSet<>(ids));
        }
    }

    private final CategoryQueryService categoryQueryService;

    @Override
    public Validator.Result<Input> validateAndSanitize(Input input) {
        var idOrKeyToId = mapToIds(input);
        var notFound = computeNotFound(idOrKeyToId, input.idOrKeys());

        return Result.ofBoth(
            input.sanitized(idOrKeyToId.values()),
            notFound.stream().map(idOrKey -> warning(WARNING_FORMAT, idOrKey, input.environmentId)).toList()
        );
    }

    private Map<String, String> mapToIds(Input input) {
        var idOrKeys = ofNullable(input.idOrKeys).orElseGet(Set::of);
        var idOrKeyToId = new HashMap<String, String>();
        for (var idOrKey : idOrKeys) {
            categoryQueryService
                .findByIdOrKey(idOrKey, input.environmentId)
                .ifPresent(category -> idOrKeyToId.put(idOrKey, category.getId()));
        }
        return idOrKeyToId;
    }

    private Set<String> computeNotFound(Map<String, String> found, Collection<String> given) {
        if (given == null) {
            return Set.of();
        }
        var notFound = new HashSet<>(given);
        notFound.removeAll(found.keySet());
        return notFound;
    }
}
