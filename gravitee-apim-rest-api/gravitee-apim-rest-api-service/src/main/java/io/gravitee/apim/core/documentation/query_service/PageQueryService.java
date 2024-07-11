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
package io.gravitee.apim.core.documentation.query_service;

import io.gravitee.apim.core.documentation.model.Page;
import java.util.List;
import java.util.Optional;

public interface PageQueryService {
    List<Page> searchByApiId(String apiId);
    Optional<Page> findHomepageByApiId(String apiId);
    List<Page> searchByApiIdAndParentId(String apiId, String parentId);
    Optional<Page> findByApiIdAndParentIdAndNameAndType(String apiId, String parentId, String name, Page.Type type);
    long countByParentIdAndIsPublished(String parentId);
    Optional<Page> findByNameAndReferenceId(String name, String referenceId);
}
