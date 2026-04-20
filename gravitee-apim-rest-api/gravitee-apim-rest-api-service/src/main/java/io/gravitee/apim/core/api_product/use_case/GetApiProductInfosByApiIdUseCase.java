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
package io.gravitee.apim.core.api_product.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.model.ApiProductInfo;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiProductInfosByApiIdUseCase {

    private final ApiProductQueryService apiProductQueryService;

    public Output execute(Input input) {
        List<ApiProductInfo> infos = apiProductQueryService
            .findByApiId(input.apiId())
            .stream()
            .map(p -> new ApiProductInfo(p.getId(), p.getName()))
            .sorted(Comparator.comparing(ApiProductInfo::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        return new Output(infos);
    }

    public record Input(String apiId) {
        public static Input of(String apiId) {
            return new Input(apiId);
        }
    }

    public record Output(List<ApiProductInfo> apiProductInfos) {}
}
