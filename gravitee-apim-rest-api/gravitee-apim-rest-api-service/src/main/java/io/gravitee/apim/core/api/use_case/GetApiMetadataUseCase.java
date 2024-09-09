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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UseCase
public class GetApiMetadataUseCase {

    private final ApiCrudService apiCrudService;
    private final ApiMetadataQueryService apiMetadataQueryService;

    public GetApiMetadataUseCase(ApiCrudService apiCrudService, ApiMetadataQueryService apiMetadataQueryService) {
        this.apiCrudService = apiCrudService;
        this.apiMetadataQueryService = apiMetadataQueryService;
    }

    public Output execute(Input input) {
        // Check that api exists with env id
        var api = apiCrudService.get(input.apiId);

        if (!api.getEnvironmentId().equals(input.envId)) {
            throw new ApiNotFoundException(input.apiId);
        }

        Map<String, ApiMetadata> apiMetadataMap = apiMetadataQueryService.findApiMetadata(api.getEnvironmentId(), input.apiId);
        var sortedApiMetadataList = this.sortApiMetadata(apiMetadataMap.values(), input.sortBy);
        var filteredApiMetadataList = this.filterApiMetadata(sortedApiMetadataList, input.filterBy);

        return new Output(filteredApiMetadataList);
    }

    private List<ApiMetadata> filterApiMetadata(List<ApiMetadata> apiMetadataList, String filterBy) {
        if (filterBy != null) {
            if (filterBy.equals("GLOBAL")) {
                return apiMetadataList.stream().filter(apiMetadata -> apiMetadata.getDefaultValue() != null).toList();
            } else if (filterBy.equals("API")) {
                return apiMetadataList.stream().filter(apiMetadata -> apiMetadata.getDefaultValue() == null).toList();
            }
        }
        return apiMetadataList;
    }

    private List<ApiMetadata> sortApiMetadata(Collection<ApiMetadata> unsortedList, String sortBy) {
        Stream<ApiMetadata> stream = unsortedList.stream();

        if (sortBy == null) {
            return stream.sorted(Comparator.comparing(ApiMetadata::getKey)).toList();
        }

        return switch (sortBy) {
            case ("value") -> stream.sorted(new ApiMetadataValueComparator()).toList();
            case ("-value") -> stream.sorted(Collections.reverseOrder(new ApiMetadataValueComparator())).toList();
            case ("name") -> stream.sorted(Comparator.comparing(ApiMetadata::getName)).toList();
            case ("-name") -> stream.sorted(Collections.reverseOrder(Comparator.comparing(ApiMetadata::getName))).toList();
            case ("format") -> stream.sorted(Comparator.comparing(ApiMetadata::getFormatToString)).toList();
            case ("-format") -> stream.sorted(Collections.reverseOrder(Comparator.comparing(ApiMetadata::getFormatToString))).toList();
            case ("-key") -> stream.sorted(Collections.reverseOrder(Comparator.comparing(ApiMetadata::getKey))).toList();
            default -> stream.sorted(Comparator.comparing(ApiMetadata::getKey)).toList();
        };
    }

    static class ApiMetadataValueComparator implements Comparator<ApiMetadata> {

        @Override
        public int compare(ApiMetadata o1, ApiMetadata o2) {
            var value1 = o1.getValue() != null ? o1.getValue() : o1.getDefaultValue();
            var value2 = o2.getValue() != null ? o2.getValue() : o2.getDefaultValue();
            return value1.compareTo(value2);
        }
    }

    public record Input(String apiId, String envId, String filterBy, String sortBy) {}

    public record Output(List<ApiMetadata> metadata) {}
}
