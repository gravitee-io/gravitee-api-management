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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageMongo;
import io.gravitee.repository.mongodb.management.internal.portal_page.PortalPageMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public Optional<PortalPage> findById(String id) {
        Optional<PortalPageMongo> mongo = internalRepository.findById(id);
        return mongo.map(mapper::map);
    }

    @Override
    public Set<PortalPage> findAll() {
        return internalRepository.findAll().stream().map(mapper::map).collect(Collectors.toSet());
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
