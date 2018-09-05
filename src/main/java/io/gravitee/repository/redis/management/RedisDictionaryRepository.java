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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.redis.management.internal.DictionaryRedisRepository;
import io.gravitee.repository.redis.management.model.RedisDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisDictionaryRepository implements DictionaryRepository {

    @Autowired
    private DictionaryRedisRepository dictionaryRedisRepository;

    @Override
    public Optional<Dictionary> findById(final String dictionaryId) throws TechnicalException {
        final RedisDictionary redisDictionary = dictionaryRedisRepository.findById(dictionaryId);
        return Optional.ofNullable(convert(redisDictionary));
    }

    @Override
    public Dictionary create(final Dictionary dictionary) throws TechnicalException {
        final RedisDictionary RedisDictionary = dictionaryRedisRepository.saveOrUpdate(convert(dictionary));
        return convert(RedisDictionary);
    }

    @Override
    public Dictionary update(final Dictionary dictionary) throws TechnicalException {
        if (dictionary == null || dictionary.getName() == null) {
            throw new IllegalStateException("Dictionary to update must have a name");
        }

        final RedisDictionary dictionaryRedis = dictionaryRedisRepository.findById(dictionary.getId());

        if (dictionaryRedis == null) {
            throw new IllegalStateException(String.format("No dictionary found with name [%s]", dictionary.getId()));
        }
        
        final RedisDictionary RedisDictionary = dictionaryRedisRepository.saveOrUpdate(convert(dictionary));
        return convert(RedisDictionary);
    }

    @Override
    public Set<Dictionary> findAll() throws TechnicalException {
        final Set<RedisDictionary> dictionarys = dictionaryRedisRepository.findAll();

        return dictionarys.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(final String dictionaryId) throws TechnicalException {
        dictionaryRedisRepository.delete(dictionaryId);
    }

    private Dictionary convert(final RedisDictionary redisDictionary) {
        if (redisDictionary == null) {
            return null;
        }
        final Dictionary dictionary = new Dictionary();
        dictionary.setId(redisDictionary.getId());
        dictionary.setName(redisDictionary.getName());
        dictionary.setDescription(redisDictionary.getDescription());
        dictionary.setType(DictionaryType.valueOf(redisDictionary.getType()));
        dictionary.setProperties(redisDictionary.getProperties());

        if (redisDictionary.getState() != null) {
            dictionary.setState(LifecycleState.valueOf(redisDictionary.getState()));
        }

        if (redisDictionary.getCreatedAt() > 0) {
            dictionary.setCreatedAt(new Date(redisDictionary.getCreatedAt()));
        }

        if (redisDictionary.getUpdatedAt() > 0) {
            dictionary.setUpdatedAt(new Date(redisDictionary.getUpdatedAt()));
        }

        if (redisDictionary.getDeployedAt() > 0) {
            dictionary.setDeployedAt(new Date(redisDictionary.getDeployedAt()));
        }

        if (redisDictionary.getProviderType() != null && redisDictionary.getProviderConfiguration() != null) {
            DictionaryProvider provider = new DictionaryProvider();
            provider.setType(redisDictionary.getProviderType());
            provider.setConfiguration(redisDictionary.getProviderConfiguration());
            dictionary.setProvider(provider);
        }

        if (redisDictionary.getTriggerRate() != 0 && redisDictionary.getTriggerUnit() != null) {
            DictionaryTrigger trigger = new DictionaryTrigger();
            trigger.setRate(redisDictionary.getTriggerRate());
            trigger.setUnit(TimeUnit.valueOf(redisDictionary.getTriggerUnit()));
            dictionary.setTrigger(trigger);
        }

        return dictionary;
    }

    private RedisDictionary convert(final Dictionary dictionary) {
        if (dictionary == null) {
            return null;
        }
        final RedisDictionary redisDictionary = new RedisDictionary();
        redisDictionary.setId(dictionary.getId());
        redisDictionary.setName(dictionary.getName());
        redisDictionary.setDescription(dictionary.getDescription());
        redisDictionary.setType(dictionary.getType().name());
        redisDictionary.setProperties(redisDictionary.getProperties());

        if (dictionary.getState() != null) {
            redisDictionary.setState(dictionary.getState().name());
        }

        if (dictionary.getProvider() != null) {
            redisDictionary.setProviderType(dictionary.getProvider().getType());
            redisDictionary.setProviderConfiguration(dictionary.getProvider().getConfiguration());
        }

        if (dictionary.getTrigger() != null) {
            redisDictionary.setTriggerRate(dictionary.getTrigger().getRate());
            redisDictionary.setTriggerUnit(dictionary.getTrigger().getUnit().name());
        }

        if (dictionary.getCreatedAt() != null) {
            redisDictionary.setCreatedAt(dictionary.getCreatedAt().getTime());
        }

        if (dictionary.getUpdatedAt() != null) {
            redisDictionary.setUpdatedAt(dictionary.getUpdatedAt().getTime());
        }

        if (dictionary.getDeployedAt() != null) {
            redisDictionary.setDeployedAt(dictionary.getDeployedAt().getTime());
        }

        return redisDictionary;
    }
}
