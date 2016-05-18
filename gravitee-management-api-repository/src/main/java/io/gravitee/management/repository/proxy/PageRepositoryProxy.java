package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class PageRepositoryProxy extends AbstractProxy<PageRepository> implements PageRepository {

    @Override
    public Collection<Page> findByApi(String s) throws TechnicalException {
        return target.findByApi(s);
    }

    @Override
    public Integer findMaxPageOrderByApi(String s) throws TechnicalException {
        return target.findMaxPageOrderByApi(s);
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

}
