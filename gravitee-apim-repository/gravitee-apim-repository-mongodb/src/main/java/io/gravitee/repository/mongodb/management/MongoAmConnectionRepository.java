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
import io.gravitee.repository.management.api.AmConnectionRepository;
import io.gravitee.repository.management.model.AmConnection;
import io.gravitee.repository.mongodb.management.internal.amconnection.AmConnectionMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AmConnectionMongo;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoAmConnectionRepository implements AmConnectionRepository {

    @Autowired
    private AmConnectionMongoRepository internalAmConnectionRepository;

    @Override
    public Optional<AmConnection> findByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("Find am connection by organization [{}]", organizationId);
        return internalAmConnectionRepository.findById(organizationId).map(this::map);
    }

    @Override
    public AmConnection create(AmConnection amConnection) throws TechnicalException {
        log.debug("Create am connection [{}]", amConnection.getOrganizationId());
        try {
            return map(internalAmConnectionRepository.insert(map(amConnection)));
        } catch (Exception e) {
            log.error("An error occurs when creating am connection [{}]", amConnection.getOrganizationId(), e);
            throw new TechnicalException("An error occurs when creating am connection");
        }
    }

    @Override
    public AmConnection update(AmConnection amConnection) throws TechnicalException {
        log.debug("Update am connection [{}]", amConnection.getOrganizationId());
        try {
            return map(internalAmConnectionRepository.save(map(amConnection)));
        } catch (Exception e) {
            log.error("An error occurs when updating am connection [{}]", amConnection.getOrganizationId(), e);
            throw new TechnicalException("An error occurs when updating am connection");
        }
    }

    @Override
    public void delete(String organizationId) throws TechnicalException {
        log.debug("Delete am connection [{}]", organizationId);
        try {
            internalAmConnectionRepository.deleteById(organizationId);
        } catch (Exception e) {
            log.error("An error occurs when deleting am connection [{}]", organizationId, e);
            throw new TechnicalException("An error occurs when deleting am connection");
        }
    }

    private AmConnectionMongo map(AmConnection amConnection) {
        if (amConnection == null) {
            return null;
        }
        AmConnectionMongo mongo = new AmConnectionMongo();
        mongo.setOrganizationId(amConnection.getOrganizationId());
        mongo.setBaseUrl(amConnection.getBaseUrl());
        mongo.setServiceAccountAccessTokenEncrypted(amConnection.getServiceAccountAccessTokenEncrypted());
        mongo.setDefaultDomainId(amConnection.getDefaultDomainId());
        mongo.setDefaultDomainHrid(amConnection.getDefaultDomainHrid());
        mongo.setGatewayUrl(amConnection.getGatewayUrl());
        mongo.setUpdatedAt(amConnection.getUpdatedAt());
        return mongo;
    }

    private AmConnection map(AmConnectionMongo mongo) {
        if (mongo == null) {
            return null;
        }
        AmConnection amConnection = new AmConnection();
        amConnection.setOrganizationId(mongo.getOrganizationId());
        amConnection.setBaseUrl(mongo.getBaseUrl());
        amConnection.setServiceAccountAccessTokenEncrypted(mongo.getServiceAccountAccessTokenEncrypted());
        amConnection.setDefaultDomainId(mongo.getDefaultDomainId());
        amConnection.setDefaultDomainHrid(mongo.getDefaultDomainHrid());
        amConnection.setGatewayUrl(mongo.getGatewayUrl());
        amConnection.setUpdatedAt(mongo.getUpdatedAt());
        return amConnection;
    }
}
