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
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PageQueryServiceInMemory implements InMemoryAlternative<Page>, PageQueryService {

    List<Page> pages;

    public PageQueryServiceInMemory() {
        this.pages = new ArrayList<>();
    }

    public PageQueryServiceInMemory(PageCrudServiceInMemory pageCrudServiceInMemory) {
        pages = pageCrudServiceInMemory.pages;
    }

    @Override
    public List<Page> searchByApiId(String apiId) {
        return pages
            .stream()
            .filter(page -> apiId.equals(page.getReferenceId()) && Page.ReferenceType.API.equals(page.getReferenceType()))
            .toList();
    }

    @Override
    public Optional<Page> findHomepageByApiId(String apiId) {
        return pages
            .stream()
            .filter(page ->
                apiId.equals(page.getReferenceId()) && Page.ReferenceType.API.equals(page.getReferenceType()) && page.isHomepage()
            )
            .findFirst();
    }

    @Override
    public List<Page> searchByApiIdAndParentId(String apiId, String parentId) {
        if (Objects.isNull(parentId) || parentId.isEmpty()) {
            return pages
                .stream()
                .filter(page ->
                    apiId.equals(page.getReferenceId()) &&
                    Page.ReferenceType.API.equals(page.getReferenceType()) &&
                    (Objects.isNull(page.getParentId()) || page.getParentId().isEmpty())
                )
                .toList();
        }
        return pages
            .stream()
            .filter(page ->
                apiId.equals(page.getReferenceId()) &&
                Page.ReferenceType.API.equals(page.getReferenceType()) &&
                parentId.equals(page.getParentId())
            )
            .toList();
    }

    @Override
    public Optional<Page> findByApiIdAndParentIdAndNameAndType(String apiId, String parentId, String name, Page.Type type) {
        var noParentId = Objects.isNull(parentId) || parentId.isEmpty();
        return pages
            .stream()
            .filter(page ->
                noParentId ? Objects.isNull(page.getParentId()) || page.getParentId().isEmpty() : parentId.equals(page.getParentId())
            )
            .filter(page ->
                apiId.equals(page.getReferenceId()) &&
                Page.ReferenceType.API.equals(page.getReferenceType()) &&
                type.equals(page.getType()) &&
                name.equals(page.getName())
            )
            .findFirst();
    }

    @Override
    public Optional<Page> findByNameAndReferenceId(String name, String referenceId) {
        return pages.stream().filter(p -> p.getName().equals(name) && p.getReferenceId().equals(referenceId)).findFirst();
    }

    @Override
    public long countByParentIdAndIsPublished(String parentId) {
        return pages.stream().filter(page -> Objects.equals(page.getParentId(), parentId) && page.isPublished()).count();
    }

    @Override
    public void initWith(List<Page> items) {
        pages = List.copyOf(items);
    }

    @Override
    public void reset() {
        pages = new ArrayList<>();
    }

    @Override
    public List<Page> storage() {
        return ImmutableList.copyOf(pages);
    }
}
