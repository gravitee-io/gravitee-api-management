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
package io.gravitee.rest.api.model.filtering;

import java.util.List;
import java.util.Map;

public class FilteredEntities<FilterableItem> {

    List<FilterableItem> filteredItems;
    Map<String, Map<String, Object>> metadata;

    public FilteredEntities(List<FilterableItem> filteredItems, Map<String, Map<String, Object>> metadata) {
        super();
        this.filteredItems = filteredItems;
        this.metadata = metadata;
    }

    public List<FilterableItem> getFilteredItems() {
        return filteredItems;
    }

    public Map<String, Map<String, Object>> getMetadata() {
        return metadata;
    }
}
