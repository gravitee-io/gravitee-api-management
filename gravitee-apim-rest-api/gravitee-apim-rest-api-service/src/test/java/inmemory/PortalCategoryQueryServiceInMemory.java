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
package inmemory;

import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.query_service.PortalCategoryQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PortalCategoryQueryServiceInMemory implements PortalCategoryQueryService, InMemoryAlternative<PortalCategory> {

    final ArrayList<PortalCategory> storage = new ArrayList<>();

    @Override
    public List<PortalCategory> findByEnvironmentId(String environmentId) {
        return storage
            .stream()
            .filter(pc -> environmentId.equals(pc.getEnvironmentId()))
            .sorted(Comparator.comparing(PortalCategory::getTitle))
            .toList();
    }

    @Override
    public void initWith(List<PortalCategory> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalCategory> storage() {
        return Collections.unmodifiableList(storage);
    }
}
