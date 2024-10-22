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
package io.gravitee.apim.core.specgen.use_case;

import static io.gravitee.apim.core.documentation.model.Page.ReferenceType.API;
import static io.gravitee.apim.core.documentation.model.Page.Visibility.*;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.Page.Type;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.apim.core.specgen.service_provider.OasProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class BuildSpecGenPageResponseUseCase {

    public static final String POWERED_BY = "poweredBy";
    public static final String NEWT_AI = "NewtAI";
    public static final Map<String, String> METADATA = Map.of(POWERED_BY, NEWT_AI);

    private final ApiSpecGenQueryService queryService;
    private final OasProvider oasProvider;

    public Maybe<Page> execute(Input input) {
        return queryService
            .rxFindByIdAndType(getExecutionContext(), input.apiId(), ApiType.PROXY)
            .observeOn(Schedulers.computation())
            .map(api -> buildPage(api, oasProvider.decorateSpecification(api, input.content())))
            .observeOn(Schedulers.io());
    }

    private static Page buildPage(ApiSpecGen api, String rawSpec) {
        return Page
            .builder()
            .referenceId(api.id())
            .referenceType(API)
            .name(api.name())
            .content(rawSpec)
            .type(Type.SWAGGER)
            .visibility(PRIVATE)
            .homepage(false)
            .published(false)
            .metadata(METADATA)
            .build();
    }

    public record Input(String apiId, String content) {}
}
