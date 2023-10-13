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

import com.google.common.collect.ImmutableList;
import io.gravitee.apim.core.documentation.crud_service.PageRevisionCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageRevision;
import io.gravitee.apim.infra.adapter.PageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.rest.api.model.PageRevisionEntity;
import io.gravitee.rest.api.service.PageRevisionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PageRevisionCrudServiceInMemory implements InMemoryAlternative<PageRevision>, PageRevisionCrudService {

    List<PageRevision> pageRevisions = new ArrayList<>();

    @Override
    public void initWith(List<PageRevision> items) {
        this.reset();
        this.pageRevisions.addAll(items);
    }

    @Override
    public void reset() {
        pageRevisions = new ArrayList<>();
    }

    @Override
    public List<PageRevision> storage() {
        return ImmutableList.copyOf(pageRevisions);
    }

    @Override
    public PageRevision create(Page page) {
        var pageRevisionToAdd = PageAdapter.INSTANCE.toPageRevision(page);
        pageRevisions.add(pageRevisionToAdd);
        return pageRevisionToAdd;
    }
}
