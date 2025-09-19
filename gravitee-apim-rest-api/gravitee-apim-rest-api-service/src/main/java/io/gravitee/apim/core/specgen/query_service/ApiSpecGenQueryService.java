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
package io.gravitee.apim.core.specgen.query_service;

import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Optional;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */

public interface ApiSpecGenQueryService {
    Optional<ApiSpecGen> findByIdAndType(ExecutionContext context, String id, ApiType type);

    default Maybe<ApiSpecGen> rxFindByIdAndType(ExecutionContext context, String id, ApiType type) {
        return Maybe.defer(() -> Maybe.just(findByIdAndType(context, id, type)))
            .flatMap(api -> api.map(Maybe::just).orElseGet(Maybe::empty))
            .subscribeOn(Schedulers.io());
    }
}
