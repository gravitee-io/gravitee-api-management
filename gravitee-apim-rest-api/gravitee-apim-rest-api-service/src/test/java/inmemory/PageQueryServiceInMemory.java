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

public class PageQueryServiceInMemory implements InMemoryAlternative<Page>, PageQueryService {

    List<Page> pages = new ArrayList<>();

    @Override
    public List<Page> searchByApiId(String apiId) {
        return pages
            .stream()
            .filter(page -> apiId.equals(page.getReferenceId()) && Page.ReferenceType.API.equals(page.getReferenceType()))
            .toList();
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
