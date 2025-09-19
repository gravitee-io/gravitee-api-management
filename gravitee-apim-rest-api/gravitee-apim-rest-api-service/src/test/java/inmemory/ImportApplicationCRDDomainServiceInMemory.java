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
package inmemory;

import io.gravitee.apim.core.application.domain_service.ImportApplicationCRDDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.Origin;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportApplicationCRDDomainServiceInMemory
    implements ImportApplicationCRDDomainService, InMemoryAlternative<BaseApplicationEntity> {

    private final Map<String, BaseApplicationEntity> storage;

    public ImportApplicationCRDDomainServiceInMemory() {
        storage = new HashMap<>();
    }

    @Override
    public BaseApplicationEntity create(NewApplicationEntity newApplicationEntity, AuditInfo auditInfo) {
        storage.computeIfPresent(newApplicationEntity.getName(), (k, application) -> {
            BaseApplicationEntity bae = toBaseApplicationEntity(newApplicationEntity);
            bae.setCreatedAt(application.getCreatedAt());
            bae.setUpdatedAt(application.getUpdatedAt());
            bae.setId(application.getId());

            return bae;
        });

        storage.computeIfAbsent(newApplicationEntity.getName(), k -> {
            BaseApplicationEntity bae = toBaseApplicationEntity(newApplicationEntity);
            bae.setId(UuidString.generateRandom());
            bae.setCreatedAt(new Date());
            bae.setUpdatedAt(new Date());
            return bae;
        });

        return storage.get(newApplicationEntity.getName());
    }

    @Override
    public BaseApplicationEntity update(String applicationId, UpdateApplicationEntity updateApplicationEntity, AuditInfo auditInfo) {
        storage.computeIfPresent(updateApplicationEntity.getName(), (k, application) -> {
            BaseApplicationEntity bae = toBaseApplicationEntity(updateApplicationEntity);
            bae.setCreatedAt(application.getCreatedAt());
            bae.setUpdatedAt(application.getUpdatedAt());
            bae.setId(application.getId());

            return bae;
        });

        return storage.get(updateApplicationEntity.getName());
    }

    @Override
    public void initWith(List<BaseApplicationEntity> items) {
        this.storage.clear();
        items.forEach(item -> storage.put(item.getName(), item));
    }

    @Override
    public void reset() {
        this.storage.clear();
    }

    @Override
    public List<BaseApplicationEntity> storage() {
        return this.storage.values().stream().toList();
    }

    private BaseApplicationEntity toBaseApplicationEntity(NewApplicationEntity newApplicationEntity) {
        var bae = new BaseApplicationEntity();
        bae.setName(newApplicationEntity.getName());
        bae.setDescription(newApplicationEntity.getDescription());
        bae.setType(newApplicationEntity.getType());
        bae.setOrigin(Origin.KUBERNETES);
        bae.setDisableMembershipNotifications(newApplicationEntity.isDisableMembershipNotifications());

        return bae;
    }

    private BaseApplicationEntity toBaseApplicationEntity(UpdateApplicationEntity updateApplicationEntity) {
        var bae = new BaseApplicationEntity();
        bae.setName(updateApplicationEntity.getName());
        bae.setDescription(updateApplicationEntity.getDescription());
        bae.setType(updateApplicationEntity.getType());
        bae.setOrigin(Origin.KUBERNETES);
        bae.setDisableMembershipNotifications(updateApplicationEntity.isDisableMembershipNotifications());

        return bae;
    }
}
