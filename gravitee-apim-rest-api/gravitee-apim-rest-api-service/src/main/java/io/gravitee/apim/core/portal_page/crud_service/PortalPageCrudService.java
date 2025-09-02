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
package io.gravitee.apim.core.portal_page.crud_service;

import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;

public interface PortalPageCrudService {
    PortalPage create(PortalPage page);
    PortalPage byPortalViewContext(PortalViewContext portalViewContext);

    PortalPage setPortalViewContextPage(PortalViewContext portalViewContext, PortalPage page);

    PortalPage getById(PageId pageId);

    boolean portalViewContextExists(PortalViewContext key);

    boolean pageIdExists(PageId pageId);
}
