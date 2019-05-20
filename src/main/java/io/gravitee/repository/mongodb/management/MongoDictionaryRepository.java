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
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryProvider;
import io.gravitee.repository.management.model.DictionaryTrigger;
import io.gravitee.repository.mongodb.management.internal.dictionary.DictionaryMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.DictionaryMongo;
import io.gravitee.repository.mongodb.management.internal.model.DictionaryProviderMongo;
import io.gravitee.repository.mongodb.management.internal.model.DictionaryTriggerMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoDictionaryRepository implements DictionaryRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoDictionaryRepository.class);

    @Autowired
    private DictionaryMongoRepository internalDictionaryRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Dictionary> findById(String id) throws TechnicalException {
        LOGGER.debug("Find dictionary by ID [{}]", id);

        DictionaryMongo page = internalDictionaryRepo.findById(id).orElse(null);
        Dictionary res = mapper.map(page, Dictionary.class);

        LOGGER.debug("Find dictionary by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public Dictionary create(Dictionary dictionary) throws TechnicalException {
        LOGGER.debug("Create dictionary [{}]", dictionary.getName());

        DictionaryMongo dictionaryMongo = mapper.map(dictionary, DictionaryMongo.class);
        DictionaryMongo createdDictionaryMongo = internalDictionaryRepo.insert(dictionaryMongo);

        Dictionary res = mapper.map(createdDictionaryMongo, Dictionary.class);

        LOGGER.debug("Create dictionary [{}] - Done", dictionary.getName());

        return res;
    }

    @Override
    public Dictionary update(Dictionary dictionary) throws TechnicalException {
        if (dictionary == null) {
            throw new IllegalStateException("Dictionary must not be null");
        }

        DictionaryMongo dictionaryMongo = internalDictionaryRepo.findById(dictionary.getId()).orElse(null);
        if (dictionaryMongo == null) {
            throw new IllegalStateException(String.format("No dictionary found with id [%s]", dictionary.getId()));
        }

        try {
            dictionaryMongo.setName(dictionary.getName());
            dictionaryMongo.setEnvironment(dictionary.getEnvironment());
            dictionaryMongo.setDescription(dictionary.getDescription());
            dictionaryMongo.setUpdatedAt(dictionary.getUpdatedAt());
            dictionaryMongo.setDeployedAt(dictionary.getDeployedAt());
            dictionaryMongo.setProperties(dictionary.getProperties());

            if (dictionary.getState() != null) {
                dictionaryMongo.setState(dictionary.getState().name());
            }

            if (dictionary.getProvider() != null) {
                dictionaryMongo.setProvider(convert(dictionary.getProvider()));
            } else {
                dictionaryMongo.setProvider(null);
            }

            if (dictionary.getTrigger() != null) {
                dictionaryMongo.setTrigger(convert(dictionary.getTrigger()));
            } else {
                dictionaryMongo.setTrigger(null);
            }

            DictionaryMongo dictionaryMongoUpdated = internalDictionaryRepo.save(dictionaryMongo);
            return mapper.map(dictionaryMongoUpdated, Dictionary.class);

        } catch (Exception e) {
            LOGGER.error("An error occured when updating dictionary", e);
            throw new TechnicalException("An error occured when updating dictionary");
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalDictionaryRepo.deleteById(id);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting dictionary [{}]", id, e);
            throw new TechnicalException("An error occured when deleting dictionary");
        }
    }

    @Override
    public Set<Dictionary> findAll() throws TechnicalException {
        LOGGER.debug("Find all dictionaries");

        List<DictionaryMongo> dictionaries = internalDictionaryRepo.findAll();
        Set<Dictionary> res = mapper.collection2set(dictionaries, DictionaryMongo.class, Dictionary.class);

        LOGGER.debug("Find all dictionaries - Done");
        return res;
    }

    private DictionaryProviderMongo convert(DictionaryProvider dictionaryProvider) {
        DictionaryProviderMongo dictionaryProviderMongo = new DictionaryProviderMongo();
        dictionaryProviderMongo.setType(dictionaryProvider.getType());
        dictionaryProviderMongo.setConfiguration(dictionaryProvider.getConfiguration());
        return dictionaryProviderMongo;
    }

    private DictionaryTriggerMongo convert(DictionaryTrigger dictionaryTrigger) {
        DictionaryTriggerMongo dictionaryTriggerMongo = new DictionaryTriggerMongo();
        dictionaryTriggerMongo.setRate(dictionaryTrigger.getRate());
        dictionaryTriggerMongo.setUnit(dictionaryTrigger.getUnit());
        return dictionaryTriggerMongo;
    }

    @Override
    public Set<Dictionary> findAllByEnvironment(String environment) throws TechnicalException {
        LOGGER.debug("Find all dictionaries by environment");

        List<DictionaryMongo> dictionaries = internalDictionaryRepo.findByEnvironment(environment);
        Set<Dictionary> res = mapper.collection2set(dictionaries, DictionaryMongo.class, Dictionary.class);

        LOGGER.debug("Find all dictionaries by environment- Done");
        return res;
    }
}
