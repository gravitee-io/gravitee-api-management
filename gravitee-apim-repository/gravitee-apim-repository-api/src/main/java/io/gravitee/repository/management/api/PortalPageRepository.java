package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PortalPage;
import java.util.List;

public interface PortalPageRepository {
    PortalPage create(PortalPage page) throws TechnicalException;
    PortalPage findById(String id) throws TechnicalException;
    List<PortalPage> findAll() throws TechnicalException;
    PortalPage update(PortalPage page) throws TechnicalException;
    void delete(String id) throws TechnicalException;

    void assignContext(String pageId, String context) throws TechnicalException;
    void removeContext(String pageId, String context) throws TechnicalException;
    List<PortalPage> findByContext(String context) throws TechnicalException;
}
