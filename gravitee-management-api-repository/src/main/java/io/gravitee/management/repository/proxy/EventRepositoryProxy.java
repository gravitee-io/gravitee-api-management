package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class EventRepositoryProxy extends AbstractProxy<EventRepository> implements EventRepository {

    @Override
    public Set<Event> findByProperty(String s, String s1) {
        return target.findByProperty(s, s1);
    }

    @Override
    public Set<Event> findByType(List<EventType> list) {
        return target.findByType(list);
    }

    @Override
    public Event create(Event event) throws TechnicalException {
        return target.create(event);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Optional<Event> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Event update(Event event) throws TechnicalException {
        return target.update(event);
    }
}
