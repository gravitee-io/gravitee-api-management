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
package io.gravitee.rest.api.service.filtering;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.filtering.FilterableItem;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import java.util.Collection;

public interface FilteringService {
    <T extends FilterableItem> FilteredEntities<T> getEntitiesOrderByNumberOfSubscriptions(
        Collection<T> items,
        Boolean excluded,
        boolean isAsc
    );

    FilteredEntities<ApiEntity> filterApis(
        final Collection<ApiEntity> apis,
        final FilterType filterType,
        final FilterType excludedFilterType
    );

    enum FilterType {
        ALL,
        FEATURED,
        MINE,
        STARRED,
        TRENDINGS,
    }
}
