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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import io.gravitee.repository.mongodb.management.internal.api.ViewMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ViewMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoViewRepository implements ViewRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoViewRepository.class);

    @Autowired
    private ViewMongoRepository internalViewRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<View> findById(String viewId) throws TechnicalException {
        LOGGER.debug("Find view by ID [{}]", viewId);

        final ViewMongo view = internalViewRepo.findById(viewId).orElse(null);

        LOGGER.debug("Find view by ID [{}] - Done", viewId);
        return Optional.ofNullable(mapper.map(view, View.class));
    }

    @Override
    public View create(View view) throws TechnicalException {
        LOGGER.debug("Create view [{}]", view.getName());

        ViewMongo viewMongo = mapper.map(view, ViewMongo.class);
        ViewMongo createdViewMongo = internalViewRepo.insert(viewMongo);

        View res = mapper.map(createdViewMongo, View.class);

        LOGGER.debug("Create view [{}] - Done", view.getName());

        return res;
    }

    @Override
    public View update(View view) throws TechnicalException {
        if (view == null || view.getName() == null) {
            throw new IllegalStateException("View to update must have a name");
        }

        final ViewMongo viewMongo = internalViewRepo.findById(view.getId()).orElse(null);

        if (viewMongo == null) {
            throw new IllegalStateException(String.format("No view found with name [%s]", view.getId()));
        }

        try {
            ViewMongo viewMongoUpdated = internalViewRepo.save(mapper.map(view, ViewMongo.class));
            return mapper.map(viewMongoUpdated, View.class);
        } catch (Exception e) {

            LOGGER.error("An error occured when updating view", e);
            throw new TechnicalException("An error occured when updating view");
        }
    }

    @Override
    public void delete(String viewId) throws TechnicalException {
        try {
            internalViewRepo.deleteById(viewId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting view [{}]", viewId, e);
            throw new TechnicalException("An error occured when deleting view");
        }
    }

    @Override
    public Set<View> findAll() throws TechnicalException {
        final List<ViewMongo> views = internalViewRepo.findAll();
        return views.stream()
                .map(viewMongo -> mapper.map(viewMongo, View.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<View> findAllByEnvironment(String environment) throws TechnicalException {
        final List<ViewMongo> views = internalViewRepo.findByEnvironment(environment);
        return views.stream()
                .map(viewMongo -> mapper.map(viewMongo, View.class))
                .collect(Collectors.toSet());
    }
}
