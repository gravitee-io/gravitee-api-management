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
package io.gravitee.apim.infra.query_service.specgen;

import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */

@Service
@Slf4j
public class ApiSpecGenQueryServiceImpl implements ApiSpecGenQueryService {

    private final ApiRepository apiRepository;

    public ApiSpecGenQueryServiceImpl(@Lazy ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    @Override
    public Optional<ApiSpecGen> findByIdAndType(ExecutionContext context, String id, ApiType type) {
        return findById(context, id).filter(api -> type.equals(api.type()));
    }

    private Optional<ApiSpecGen> findById(ExecutionContext context, String id) {
        try {
            return apiRepository
                .findById(id)
                .filter(api -> api.getEnvironmentId().equals(context.getEnvironmentId()))
                .map(api ->
                    new ApiSpecGen(
                        api.getId(),
                        api.getName(),
                        api.getDescription(),
                        api.getVersion(),
                        api.getType(),
                        api.getEnvironmentId()
                    )
                );
        } catch (TechnicalException e) {
            log.error("An unexpected error has occurred", e);
            return Optional.empty();
        }
    }
}
