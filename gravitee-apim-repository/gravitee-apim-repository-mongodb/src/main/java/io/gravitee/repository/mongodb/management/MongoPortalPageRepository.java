package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageMongo;
import io.gravitee.repository.mongodb.management.internal.portal_page.PortalPageMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MongoPortalPageRepository implements PortalPageRepository {
    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private PortalPageMongoRepository internalRepository;

    @Override
    public PortalPage create(PortalPage page) {
        PortalPageMongo mongo = mapper.map(page);
        PortalPageMongo saved = internalRepository.save(mongo);
        return mapper.map(saved);
    }

    @Override
    public PortalPage findById(String id) {
        Optional<PortalPageMongo> mongo = internalRepository.findById(id);
        return mongo.map(mapper::map).orElse(null);
    }

    @Override
    public List<PortalPage> findAll() {
        return internalRepository.findAll().stream().map(mapper::map).collect(Collectors.toList());
    }

    @Override
    public PortalPage update(PortalPage page) {
        PortalPageMongo mongo = mapper.map(page);
        PortalPageMongo saved = internalRepository.save(mongo);
        return mapper.map(saved);
    }

    @Override
    public void delete(String id) {
        internalRepository.deleteById(id);
    }

    @Override
    public void assignContext(String pageId, String context) {
        PortalPageMongo mongo = internalRepository.findById(pageId).orElse(null);
        if (mongo != null && (mongo.getContexts() == null || !mongo.getContexts().contains(context))) {
            if (mongo.getContexts() == null) {
                mongo.setContexts(new java.util.ArrayList<>());
            }
            mongo.getContexts().add(context);
            internalRepository.save(mongo);
        }
    }

    @Override
    public void removeContext(String pageId, String context) {
        PortalPageMongo mongo = internalRepository.findById(pageId).orElse(null);
        if (mongo != null && mongo.getContexts() != null && mongo.getContexts().contains(context)) {
            mongo.getContexts().remove(context);
            internalRepository.save(mongo);
        }
    }

    @Override
    public List<PortalPage> findByContext(String context) {
        return internalRepository.findByContext(context).stream().map(mapper::map).collect(Collectors.toList());
    }
}
