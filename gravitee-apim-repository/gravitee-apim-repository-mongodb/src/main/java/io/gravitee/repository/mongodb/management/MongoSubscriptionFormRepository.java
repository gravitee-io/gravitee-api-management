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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionFormMongo;
import io.gravitee.repository.mongodb.management.internal.subscriptionform.SubscriptionFormMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MongoDB implementation of the SubscriptionFormRepository.
 *
 * @author Gravitee.io Team
 */
@CustomLog
@Component
public class MongoSubscriptionFormRepository implements SubscriptionFormRepository {

    @Autowired
    private SubscriptionFormMongoRepository internalSubscriptionFormRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<SubscriptionForm> findById(String subscriptionFormId) throws TechnicalException {
        log.debug("Find subscription form by ID [{}]", subscriptionFormId);
        Optional<SubscriptionForm> result = internalSubscriptionFormRepo.findById(subscriptionFormId).map(mapper::map);
        log.debug("Find subscription form by ID [{}] - Done", subscriptionFormId);
        return result;
    }

    @Override
    public Optional<SubscriptionForm> findByIdAndEnvironmentId(String id, String environmentId) throws TechnicalException {
        log.debug("Find subscription form by ID [{}] and environment ID [{}]", id, environmentId);
        Optional<SubscriptionForm> result = internalSubscriptionFormRepo.findByIdAndEnvironmentId(id, environmentId).map(mapper::map);
        log.debug("Find subscription form by ID [{}] and environment ID [{}] - Done", id, environmentId);
        return result;
    }

    @Override
    public Optional<SubscriptionForm> findByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Find subscription form by environment ID [{}]", environmentId);
        Optional<SubscriptionForm> result = internalSubscriptionFormRepo.findByEnvironmentId(environmentId).map(mapper::map);
        log.debug("Find subscription form by environment ID [{}] - Done", environmentId);
        return result;
    }

    @Override
    public SubscriptionForm create(SubscriptionForm subscriptionForm) throws TechnicalException {
        log.debug("Create subscription form [{}]", subscriptionForm.getId());

        SubscriptionFormMongo subscriptionFormMongo = mapper.map(subscriptionForm);
        SubscriptionFormMongo createdSubscriptionForm = internalSubscriptionFormRepo.insert(subscriptionFormMongo);

        SubscriptionForm result = mapper.map(createdSubscriptionForm);

        log.debug("Create subscription form [{}] - Done", subscriptionForm.getId());
        return result;
    }

    @Override
    public SubscriptionForm update(SubscriptionForm subscriptionForm) throws TechnicalException {
        if (subscriptionForm == null) {
            throw new TechnicalException("Subscription form must not be null");
        }

        SubscriptionFormMongo existingMongo = internalSubscriptionFormRepo.findById(subscriptionForm.getId()).orElse(null);
        if (existingMongo == null) {
            throw new TechnicalException(String.format("Subscription form not found with id [%s]", subscriptionForm.getId()));
        }

        try {
            SubscriptionFormMongo subscriptionFormMongo = mapper.map(subscriptionForm);
            SubscriptionFormMongo updatedMongo = internalSubscriptionFormRepo.save(subscriptionFormMongo);
            return mapper.map(updatedMongo);
        } catch (Exception e) {
            log.error("An error occurred when updating subscription form", e);
            throw new TechnicalException("An error occurred when updating subscription form");
        }
    }

    @Override
    public void delete(String subscriptionFormId) throws TechnicalException {
        try {
            log.debug("Delete subscription form [{}]", subscriptionFormId);
            internalSubscriptionFormRepo.deleteById(subscriptionFormId);
            log.debug("Delete subscription form [{}] - Done", subscriptionFormId);
        } catch (Exception e) {
            log.error("An error occurred when deleting subscription form [{}]", subscriptionFormId, e);
            throw new TechnicalException("An error occurred when deleting subscription form");
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        try {
            log.debug("Delete subscription form by environment ID [{}]", environmentId);
            internalSubscriptionFormRepo.deleteByEnvironmentId(environmentId);
            log.debug("Delete subscription form by environment ID [{}] - Done", environmentId);
        } catch (Exception e) {
            log.error("An error occurred when deleting subscription form by environment ID [{}]", environmentId, e);
            throw new TechnicalException("An error occurred when deleting subscription form by environment ID");
        }
    }

    @Override
    public Set<SubscriptionForm> findAll() throws TechnicalException {
        return internalSubscriptionFormRepo
            .findAll()
            .stream()
            .map(mongo -> mapper.map(mongo))
            .collect(Collectors.toSet());
    }
}
