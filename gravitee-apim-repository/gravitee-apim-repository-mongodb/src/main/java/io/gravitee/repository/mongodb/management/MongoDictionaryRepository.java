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
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryProvider;
import io.gravitee.repository.management.model.DictionaryTrigger;
import io.gravitee.repository.mongodb.management.internal.dictionary.DictionaryMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.DictionaryMongo;
import io.gravitee.repository.mongodb.management.internal.model.DictionaryProviderMongo;
import io.gravitee.repository.mongodb.management.internal.model.DictionaryTriggerMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoDictionaryRepository implements DictionaryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDictionaryRepository.class);

    private static final String DOT_REPLACEMENT = "\\$#\\$";
    private static final String DOT = "\\.";
    private static final Pattern DOT_REPLACEMENT_PATTERN = Pattern.compile(DOT_REPLACEMENT);
    private static final Pattern DOT_PATTERN = Pattern.compile(DOT);

    @Autowired
    private DictionaryMongoRepository internalDictionaryRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Dictionary> findById(String id) throws TechnicalException {
        LOGGER.debug("Find dictionary by ID [{}]", id);

        DictionaryMongo page = internalDictionaryRepo.findById(id).orElse(null);
        Dictionary res = mapper.map(page);

        if (res != null && res.getProperties() != null) {
            final Map<String, String> properties = new HashMap<>(res.getProperties().size());
            res.getProperties().forEach((key, value) -> properties.put(computeOriginalKey(key), value));
            res.setProperties(properties);
        }

        LOGGER.debug("Find dictionary by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public Dictionary create(Dictionary dictionary) throws TechnicalException {
        LOGGER.debug("Create dictionary [{}]", dictionary.getName());

        DictionaryMongo dictionaryMongo = mapper.map(dictionary);

        if (dictionaryMongo.getProperties() != null) {
            final Map<String, String> properties = new HashMap<>(dictionaryMongo.getProperties().size());
            dictionaryMongo.getProperties().forEach((key, value) -> properties.put(computeMongoDBCompliantKey(key), value));
            dictionaryMongo.setProperties(properties);
        }

        DictionaryMongo createdDictionaryMongo = internalDictionaryRepo.insert(dictionaryMongo);

        Dictionary res = mapper.map(createdDictionaryMongo);

        if (res != null && res.getProperties() != null) {
            final Map<String, String> properties = new HashMap<>(res.getProperties().size());
            res.getProperties().forEach((key, value) -> properties.put(computeOriginalKey(key), value));
            res.setProperties(properties);
        }

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
            dictionaryMongo.setEnvironmentId(dictionary.getEnvironmentId());
            dictionaryMongo.setDescription(dictionary.getDescription());
            dictionaryMongo.setUpdatedAt(dictionary.getUpdatedAt());
            dictionaryMongo.setDeployedAt(dictionary.getDeployedAt());

            if (dictionary.getProperties() != null) {
                final Map<String, String> properties = new HashMap<>(dictionary.getProperties().size());
                dictionary.getProperties().forEach((key, value) -> properties.put(computeMongoDBCompliantKey(key), value));
                dictionaryMongo.setProperties(properties);
            }

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

            final Dictionary res = mapper.map(dictionaryMongoUpdated);

            if (res != null && res.getProperties() != null) {
                final Map<String, String> properties = new HashMap<>(res.getProperties().size());
                res.getProperties().forEach((key, value) -> properties.put(computeOriginalKey(key), value));
                res.setProperties(properties);
            }

            return res;
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
        Set<Dictionary> res = mapper.mapDictionaries(dictionaries);

        LOGGER.debug("Find all dictionaries - Done");
        return res;
    }

    /**
     * Compute a key compliant with MongoDB i.e. without dots in the name.
     * The key can be converted back using {@link #computeOriginalKey(String)}.
     *
     * @see #computeOriginalKey(String)
     * @param input The input key
     * @return A key compliant with MongoDB
     */
    private static String computeMongoDBCompliantKey(String input) {
        return DOT_PATTERN.matcher(input).replaceAll(DOT_REPLACEMENT);
    }

    /**
     * Compute the original version of a key updated to be compliant with MongoDB.
     * The original conversion is done using {@link #computeMongoDBCompliantKey(String)}.
     *
     * @see #computeMongoDBCompliantKey(String)
     * @param key The key converted and used so save in the database
     * @return The original key
     */
    private static String computeOriginalKey(String key) {
        return DOT_REPLACEMENT_PATTERN.matcher(key).replaceAll(DOT);
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
    public Set<Dictionary> findAllByEnvironments(Set<String> environments) throws TechnicalException {
        LOGGER.debug("Find all dictionaries by environment");

        if (CollectionUtils.isEmpty(environments)) {
            return findAll();
        }

        List<DictionaryMongo> dictionaries = internalDictionaryRepo.findByEnvironments(environments);
        Set<Dictionary> res = mapper.mapDictionaries(dictionaries);

        LOGGER.debug("Find all dictionaries by environment- Done");
        return res;
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("Delete dictionaries by environmentId: {}", environmentId);
        try {
            final var dictionaries = internalDictionaryRepo
                .deleteByEnvironmentId(environmentId)
                .stream()
                .map(DictionaryMongo::getId)
                .toList();
            LOGGER.debug("Delete dictionaries by environmentId: {} - Done", environmentId);
            return dictionaries;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete dictionaries by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete dictionaries by environmentId");
        }
    }
}
