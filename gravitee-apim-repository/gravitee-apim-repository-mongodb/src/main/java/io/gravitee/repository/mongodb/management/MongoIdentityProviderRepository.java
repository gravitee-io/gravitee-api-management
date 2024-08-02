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
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.mongodb.management.internal.identityprovider.IdentityProviderMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.IdentityProviderMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoIdentityProviderRepository implements IdentityProviderRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoIdentityProviderRepository.class);

    @Autowired
    private IdentityProviderMongoRepository internalIdentityProviderRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<IdentityProvider> findById(String id) throws TechnicalException {
        LOGGER.debug("Find identity provider by ID [{}]", id);

        IdentityProviderMongo identityProvider = internalIdentityProviderRepository.findById(id).orElse(null);

        LOGGER.debug("Find identity provider by ID [{}] - Done", id);
        return Optional.ofNullable(map(identityProvider));
    }

    @Override
    public IdentityProvider create(IdentityProvider identityProvider) throws TechnicalException {
        LOGGER.debug("Create identity provider [{}]", identityProvider.getName());

        IdentityProviderMongo identityProviderMongo = map(identityProvider);
        IdentityProviderMongo createdIdentityProviderMongo = internalIdentityProviderRepository.insert(identityProviderMongo);

        LOGGER.debug("Create identity provider [{}] - Done", identityProvider.getName());

        return map(createdIdentityProviderMongo);
    }

    @Override
    public IdentityProvider update(IdentityProvider identityProvider) throws TechnicalException {
        if (identityProvider == null) {
            throw new IllegalStateException("Identity provider must not be null");
        }

        IdentityProviderMongo identityProviderMongo = internalIdentityProviderRepository.findById(identityProvider.getId()).orElse(null);
        if (identityProviderMongo == null) {
            throw new IllegalStateException(String.format("No identity provider found with id [%s]", identityProvider.getId()));
        }

        try {
            IdentityProviderMongo providerMongo = map(identityProvider);
            identityProviderMongo.setName(identityProvider.getName());
            identityProviderMongo.setEnabled(identityProvider.isEnabled());
            identityProviderMongo.setDescription(identityProvider.getDescription());
            identityProviderMongo.setUpdatedAt(identityProvider.getUpdatedAt());
            identityProviderMongo.setConfiguration(identityProvider.getConfiguration());
            identityProviderMongo.setGroupMappings(providerMongo.getGroupMappings());
            identityProviderMongo.setRoleMappings(providerMongo.getRoleMappings());
            identityProviderMongo.setUserProfileMapping(identityProvider.getUserProfileMapping());
            identityProviderMongo.setEmailRequired(identityProvider.getEmailRequired());
            identityProviderMongo.setSyncMappings(identityProvider.getSyncMappings());

            IdentityProviderMongo identityProviderMongoUpdated = internalIdentityProviderRepository.save(identityProviderMongo);
            return map(identityProviderMongoUpdated);
        } catch (Exception e) {
            LOGGER.error("An error occurs when updating identity provider", e);
            throw new TechnicalException("An error occurs when updating identity provider");
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalIdentityProviderRepository.deleteById(id);
        } catch (Exception e) {
            LOGGER.error("An error occurs when deleting identity provider [{}]", id, e);
            throw new TechnicalException("An error occurs when deleting identity provider");
        }
    }

    @Override
    public Set<IdentityProvider> findAll() throws TechnicalException {
        LOGGER.debug("Find all identity providers");

        Set<IdentityProvider> res = internalIdentityProviderRepository.findAll().stream().map(this::map).collect(Collectors.toSet());

        LOGGER.debug("Find all identity providers - Done");
        return res;
    }

    private IdentityProviderMongo map(IdentityProvider identityProvider) {
        if (identityProvider == null) {
            return null;
        }

        IdentityProviderMongo identityProviderMongo = mapper.map(identityProvider);

        if (identityProvider.getGroupMappings() != null) {
            Map<String, String[]> groupMappings = new HashMap<>(identityProvider.getGroupMappings().size());
            for (Map.Entry<String, String[]> groupEntry : identityProvider.getGroupMappings().entrySet()) {
                groupMappings.put(new String(Base64.getEncoder().encode(groupEntry.getKey().getBytes())), groupEntry.getValue());
            }
            identityProviderMongo.setGroupMappings(groupMappings);
        }

        if (identityProvider.getRoleMappings() != null) {
            Map<String, String[]> roleMappings = new HashMap<>(identityProvider.getRoleMappings().size());
            for (Map.Entry<String, String[]> roleEntry : identityProvider.getRoleMappings().entrySet()) {
                roleMappings.put(new String(Base64.getEncoder().encode(roleEntry.getKey().getBytes())), roleEntry.getValue());
            }
            identityProviderMongo.setRoleMappings(roleMappings);
        }

        return identityProviderMongo;
    }

    private IdentityProvider map(IdentityProviderMongo identityProviderMongo) {
        if (identityProviderMongo == null) {
            return null;
        }

        IdentityProvider identityProvider = mapper.map(identityProviderMongo);

        if (identityProviderMongo.getGroupMappings() != null) {
            identityProviderMongo
                .getGroupMappings()
                .forEach((condition, groups) -> {
                    identityProvider
                        .getGroupMappings()
                        .put(new String(Base64.getDecoder().decode(condition)), identityProviderMongo.getGroupMappings().get(condition));
                    identityProvider.getGroupMappings().remove(condition);
                });
        }

        if (identityProviderMongo.getRoleMappings() != null) {
            identityProviderMongo
                .getRoleMappings()
                .forEach((condition, roles) -> {
                    identityProvider
                        .getRoleMappings()
                        .put(new String(Base64.getDecoder().decode(condition)), identityProviderMongo.getRoleMappings().get(condition));
                    identityProvider.getRoleMappings().remove(condition);
                });
        }

        return identityProvider;
    }

    @Override
    public Set<IdentityProvider> findAllByOrganizationId(String organizationId) throws TechnicalException {
        LOGGER.debug("Find all identity providers by organization {}", organizationId);

        Set<IdentityProvider> res = internalIdentityProviderRepository
            .findByOrganizationId(organizationId)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());

        LOGGER.debug("Find all identity providers by organization {} - Done", organizationId);
        return res;
    }

    @Override
    public List<String> deleteByOrganizationId(String organizationId) throws TechnicalException {
        LOGGER.debug("Delete all identity providers by organizationId: {}", organizationId);
        try {
            final var res = internalIdentityProviderRepository
                .deleteByOrganizationId(organizationId)
                .stream()
                .map(IdentityProviderMongo::getId)
                .toList();
            LOGGER.debug("Delete all identity providers by organizationId: {} - Done", organizationId);
            return res;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete identity providers by organizationId: {}", organizationId, ex);
            throw new TechnicalException("Failed to delete identity providers by organizationId");
        }
    }
}
