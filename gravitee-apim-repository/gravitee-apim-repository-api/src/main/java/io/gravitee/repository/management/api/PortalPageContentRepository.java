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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PortalPageContent;
import java.util.List;

public interface PortalPageContentRepository extends CrudRepository<PortalPageContent, String> {
    List<PortalPageContent> findAllByType(PortalPageContent.Type type) throws TechnicalException;

    PortalPageContent findByPageId(String pageId) throws TechnicalException;

    void deleteByType(PortalPageContent.Type type) throws TechnicalException;
}
