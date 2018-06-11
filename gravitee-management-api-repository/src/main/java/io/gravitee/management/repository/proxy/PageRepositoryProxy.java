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
package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume GILLON 
 */
@Component
public class PageRepositoryProxy extends AbstractProxy<PageRepository> implements PageRepository {

    @Override
    public Collection<Page> findApiPageByApiId(String s) throws TechnicalException {
        return target.findApiPageByApiId(s);
    }

    @Override
    public Collection<Page> findApiPageByApiIdAndHomepage(String apiId, boolean isHomepage) throws TechnicalException {
        return target.findApiPageByApiIdAndHomepage(apiId, isHomepage);
    }

    @Override
    public Integer findMaxApiPageOrderByApiId(String s) throws TechnicalException {
        return target.findMaxApiPageOrderByApiId(s);
    }

    @Override
    public Page create(Page page) throws TechnicalException {
        return target.create(page);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Optional<Page> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Page update(Page page) throws TechnicalException {
        return target.update(page);
    }

    @Override
    public Collection<Page> findPortalPageByHomepage(boolean isHomepage) throws TechnicalException {
        return target.findPortalPageByHomepage(isHomepage);
    }

    @Override
    public Collection<Page> findPortalPages() throws TechnicalException {
        return target.findPortalPages();
    }

    @Override
    public Integer findMaxPortalPageOrder() throws TechnicalException {
        return target.findMaxPortalPageOrder();
    }

    @Override
    public void removeAllFolderParentWith(String pageId, String apiId) throws TechnicalException {
        target.removeAllFolderParentWith(pageId, apiId);
    }
}
