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
import io.gravitee.repository.management.model.PortalPage;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PortalPageRepository {
    PortalPage create(PortalPage page) throws TechnicalException;
    Optional<PortalPage> findById(String id) throws TechnicalException;
    Set<PortalPage> findAll() throws TechnicalException;
    PortalPage update(PortalPage page) throws TechnicalException;
    void delete(String id) throws TechnicalException;

    void assignContext(String pageId, String context) throws TechnicalException;
    void removeContext(String pageId, String context) throws TechnicalException;
    List<PortalPage> findByContext(String context) throws TechnicalException;
}
