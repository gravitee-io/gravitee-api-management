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
package io.gravitee.apim.infra.crud_service.api;

import io.gravitee.apim.core.api.crud_service.ApiDefinitionCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class ApiDefinitionCrudServiceImpl implements ApiDefinitionCrudService {

    private final ApiRepository apiRepository;

    public ApiDefinitionCrudServiceImpl(@Lazy ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    @Override
    public boolean replaceDefinition(Api api, ZonedDateTime expectedUpdatedAt) {
        try {
            log.debug("Replace the definition of api {}", api.getId());
            return apiRepository.updateDefinition(
                api.getId(),
                ApiAdapter.INSTANCE.serializeApiDefinition(api),
                guardiansOf(api),
                toDate(api.getUpdatedAt()),
                toDate(expectedUpdatedAt)
            );
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(String.format("An error occurs while replacing the definition of api %s", api.getId()), e);
        }
    }

    /**
     * The index is read out of the definition being written, by the same rule as everywhere else, rather than taken
     * from the caller: the two then cannot disagree, whoever writes a definition. Only an HTTP v4 definition can
     * carry a {@code guardian-policy} step; every other kind runs no Guardian and indexes none.
     */
    private static Set<String> guardiansOf(Api api) {
        return api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api definition
            ? GuardianSteps.guardianIdsOf(definition.getInternalFlow())
            : Set.of();
    }

    private static Date toDate(ZonedDateTime at) {
        return at == null ? null : Date.from(at.toInstant());
    }
}
