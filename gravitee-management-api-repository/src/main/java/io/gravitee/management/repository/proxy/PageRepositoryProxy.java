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

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume GILLON 
 */
@Component
public class PageRepositoryProxy extends AbstractProxy<PageRepository> implements PageRepository {


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
    public List<Page> search(PageCriteria criteria) throws TechnicalException {
        return target.search(criteria);
    }

    @Override
    public Integer findMaxPageReferenceIdAndReferenceTypeOrder(String referenceId, PageReferenceType referenceType)
            throws TechnicalException {
        return target.findMaxPageReferenceIdAndReferenceTypeOrder(referenceId, referenceType);
    }
}
