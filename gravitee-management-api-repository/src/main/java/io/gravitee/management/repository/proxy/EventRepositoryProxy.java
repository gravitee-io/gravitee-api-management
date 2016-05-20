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
