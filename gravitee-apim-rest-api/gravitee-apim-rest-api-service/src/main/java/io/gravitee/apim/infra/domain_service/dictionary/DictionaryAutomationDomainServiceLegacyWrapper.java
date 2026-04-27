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
package io.gravitee.apim.infra.domain_service.dictionary;

import io.gravitee.apim.core.dictionary.domain_service.DictionaryAutomationDomainService;
import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.apim.core.dictionary.model.DictionaryProvider;
import io.gravitee.apim.core.dictionary.model.DictionaryTrigger;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.impl.configuration.dictionary.DictionaryNotFoundException;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DictionaryAutomationDomainServiceLegacyWrapper implements DictionaryAutomationDomainService {

    private final DictionaryService dictionaryService;

    @Override
    public DictionaryEntity create(ExecutionContext executionContext, Dictionary dictionary) {
        return dictionaryService.create(executionContext, toNewDictionaryEntity(dictionary));
    }

    @Override
    public DictionaryEntity update(ExecutionContext executionContext, String id, Dictionary dictionary) {
        return dictionaryService.update(executionContext, id, toUpdateDictionaryEntity(dictionary));
    }

    public Optional<DictionaryEntity> findById(ExecutionContext executionContext, String id) {
        try {
            return Optional.of(dictionaryService.findById(executionContext, id));
        } catch (DictionaryNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String id) {
        dictionaryService.delete(executionContext, id);
    }

    @Override
    public DictionaryEntity handleDeployment(ExecutionContext executionContext, DictionaryEntity dictionary, boolean deploy) {
        if (dictionary.getType() == DictionaryType.MANUAL) {
            if (deploy) {
                return dictionaryService.deploy(executionContext, dictionary.getId());
            } else {
                return dictionaryService.undeploy(executionContext, dictionary.getId());
            }
        } else if (dictionary.getType() == DictionaryType.DYNAMIC) {
            if (deploy && !isStarted(dictionary)) {
                return dictionaryService.start(executionContext, dictionary.getId());
            } else if (!deploy && isStarted(dictionary)) {
                return dictionaryService.stop(executionContext, dictionary.getId());
            }
        }
        return dictionary;
    }

    private static boolean isStarted(DictionaryEntity dictionary) {
        return dictionary.getState() != null && Objects.equals(dictionary.getState().name(), Lifecycle.State.STARTED.name());
    }

    private static NewDictionaryEntity toNewDictionaryEntity(Dictionary dictionary) {
        NewDictionaryEntity entity = new NewDictionaryEntity();
        entity.setId(dictionary.getId());
        entity.setKey(dictionary.getHrid());
        entity.setName(dictionary.getName());
        entity.setDescription(dictionary.getDescription());
        entity.setType(toEntityType(dictionary.getType()));
        entity.setProperties(dictionary.getProperties());
        entity.setProvider(toEntity(dictionary.getProvider()));
        entity.setTrigger(toEntity(dictionary.getTrigger()));
        return entity;
    }

    private static UpdateDictionaryEntity toUpdateDictionaryEntity(Dictionary dictionary) {
        UpdateDictionaryEntity entity = new UpdateDictionaryEntity();
        entity.setName(dictionary.getName());
        entity.setDescription(dictionary.getDescription());
        entity.setType(toEntityType(dictionary.getType()));
        entity.setProperties(dictionary.getProperties());
        entity.setProvider(toEntity(dictionary.getProvider()));
        entity.setTrigger(toEntity(dictionary.getTrigger()));
        return entity;
    }

    private static DictionaryType toEntityType(io.gravitee.apim.core.dictionary.model.DictionaryType type) {
        if (type == null) return null;
        return DictionaryType.valueOf(type.name());
    }

    private static DictionaryProviderEntity toEntity(DictionaryProvider provider) {
        if (provider == null) return null;
        DictionaryProviderEntity entity = new DictionaryProviderEntity();
        entity.setType(provider.getType());
        entity.setConfiguration(provider.getConfiguration());
        return entity;
    }

    private static DictionaryTriggerEntity toEntity(DictionaryTrigger trigger) {
        if (trigger == null) return null;
        DictionaryTriggerEntity entity = new DictionaryTriggerEntity();
        entity.setRate(trigger.getRate());
        entity.setUnit(trigger.getUnit());
        return entity;
    }
}
