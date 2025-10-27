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
package io.gravitee.apim.core.documentation.crud_service;

import io.gravitee.apim.core.documentation.model.Page;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PageCrudService {
    Page createDocumentationPage(Page page);
    Page updateDocumentationPage(Page pageToUpdate);
    Page get(String id);
    Optional<Page> findById(String id);
    void delete(String id);
    void unsetHomepage(Collection<String> ids);
    List<Page> findByApiId(String apiId);
    void updateCrossIds(List<Page> pages);
}
