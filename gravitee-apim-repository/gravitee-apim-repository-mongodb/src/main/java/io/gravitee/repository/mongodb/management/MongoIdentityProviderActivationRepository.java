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
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.model.IdentityProviderActivation;
import io.gravitee.repository.management.model.IdentityProviderActivationReferenceType;
import io.gravitee.repository.mongodb.management.internal.identityprovideractivation.IdentityProviderActivationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.IdentityProviderActivationMongo;
import io.gravitee.repository.mongodb.management.internal.model.IdentityProviderActivationPkMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
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
public class MongoIdentityProviderActivationRepository implements IdentityProviderActivationRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoIdentityProviderActivationRepository.class);

    @Autowired
    private IdentityProviderActivationMongoRepository internalIdentityProviderActivationRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<IdentityProviderActivation> findById(
        String identityProviderId,
        String referenceId,
        IdentityProviderActivationReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug("Find identity provider activations by Id [{}, {}, {}]", identityProviderId, referenceId, referenceType);

        IdentityProviderActivationMongo identityProviderActivation = internalIdentityProviderActivationRepository
            .findById(new IdentityProviderActivationPkMongo(identityProviderId, referenceId, referenceType.name()))
            .orElse(null);

        LOGGER.debug("Find identity provider activation by Id [{}, {}, {}] - Done", identityProviderId, referenceId, referenceType);

        return Optional.ofNullable(map(identityProviderActivation));
    }

    @Override
    public Set<IdentityProviderActivation> findAll() throws TechnicalException {
        LOGGER.debug("Find all identity provider activations");

        Set<IdentityProviderActivation> res = internalIdentityProviderActivationRepository
            .findAll()
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());

        LOGGER.debug("Find all identity provider activations - Done");
        return res;
    }

    @Override
    public Set<IdentityProviderActivation> findAllByIdentityProviderId(String identityProviderId) throws TechnicalException {
        LOGGER.debug("Find identity provider activations by Idp Id [{}]", identityProviderId);

        Set<IdentityProviderActivation> result = internalIdentityProviderActivationRepository
            .findAllByIdentityProviderId(identityProviderId)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        LOGGER.debug("Find identity provider activations by Idp Id [{}] - Done", identityProviderId);

        return result;
    }

    @Override
    public Set<IdentityProviderActivation> findAllByReferenceIdAndReferenceType(
        String referenceId,
        IdentityProviderActivationReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug("Find identity provider activations by ref ID and ref Type [{}, {}]", referenceId, referenceType);

        Set<IdentityProviderActivation> result = internalIdentityProviderActivationRepository
            .findAllByReferenceIdAndReferenceType(referenceId, referenceType.name())
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        LOGGER.debug("Find identity provider activations by ref ID and ref Type [{}, {}] - Done", referenceId, referenceType);

        return result;
    }

    @Override
    public IdentityProviderActivation create(IdentityProviderActivation identityProviderActivation) throws TechnicalException {
        LOGGER.debug(
            "Create identity provider activation [{}, {}, {}]",
            identityProviderActivation.getIdentityProviderId(),
            identityProviderActivation.getReferenceId(),
            identityProviderActivation.getReferenceType()
        );

        IdentityProviderActivationMongo identityProviderActivationMongo = map(identityProviderActivation);
        IdentityProviderActivationMongo createdIdentityProviderActivationMongo = internalIdentityProviderActivationRepository.insert(
            identityProviderActivationMongo
        );

        LOGGER.debug(
            "Create identity provider activation [{}, {}, {}] - Done",
            identityProviderActivation.getIdentityProviderId(),
            identityProviderActivation.getReferenceId(),
            identityProviderActivation.getReferenceType()
        );

        return map(createdIdentityProviderActivationMongo);
    }

    @Override
    public void delete(String identityProviderId, String referenceId, IdentityProviderActivationReferenceType referenceType)
        throws TechnicalException {
        try {
            internalIdentityProviderActivationRepository.deleteById(
                new IdentityProviderActivationPkMongo(identityProviderId, referenceId, referenceType.name())
            );
        } catch (Exception e) {
            LOGGER.error(
                "An error occurs when deleting identity provider activation [{}, {}, {}]",
                identityProviderId,
                referenceId,
                referenceType.name(),
                e
            );
            throw new TechnicalException("An error occurs when deleting identity provider activation");
        }
    }

    @Override
    public void deleteByIdentityProviderId(String identityProviderId) throws TechnicalException {
        try {
            internalIdentityProviderActivationRepository.deleteByIdentityProviderId(identityProviderId);
        } catch (Exception e) {
            LOGGER.error("An error occurs when deleting identity provider activation by identity provider id[{}]", identityProviderId, e);
            throw new TechnicalException("An error occurs when deleting provider activation by identity provider id");
        }
    }

    @Override
    public void deleteByReferenceIdAndReferenceType(String referenceId, IdentityProviderActivationReferenceType referenceType)
        throws TechnicalException {
        try {
            internalIdentityProviderActivationRepository.deleteByReferenceIdAndReferenceType(referenceId, referenceType.name());
        } catch (Exception e) {
            LOGGER.error("An error occurs when deleting identity provider activation by ref [{}, {}]", referenceId, referenceType, e);
            throw new TechnicalException("An error occurs when deleting identity provider activation by ref");
        }
    }

    private IdentityProviderActivationMongo map(IdentityProviderActivation identityProviderActivation) {
        if (identityProviderActivation == null) {
            return null;
        }

        IdentityProviderActivationPkMongo id = new IdentityProviderActivationPkMongo();
        id.setIdentityProviderId(identityProviderActivation.getIdentityProviderId());
        id.setReferenceId(identityProviderActivation.getReferenceId());
        id.setReferenceType(identityProviderActivation.getReferenceType().name());

        IdentityProviderActivationMongo identityProviderActivationMongo = new IdentityProviderActivationMongo();
        identityProviderActivationMongo.setId(id);
        identityProviderActivationMongo.setCreatedAt(identityProviderActivation.getCreatedAt());

        return identityProviderActivationMongo;
    }

    private IdentityProviderActivation map(IdentityProviderActivationMongo identityProviderActivationMongo) {
        if (identityProviderActivationMongo == null) {
            return null;
        }

        IdentityProviderActivation identityProviderActivation = new IdentityProviderActivation();
        identityProviderActivation.setIdentityProviderId(identityProviderActivationMongo.getId().getIdentityProviderId());
        identityProviderActivation.setReferenceId(identityProviderActivationMongo.getId().getReferenceId());
        identityProviderActivation.setReferenceType(
            IdentityProviderActivationReferenceType.valueOf(identityProviderActivationMongo.getId().getReferenceType())
        );
        identityProviderActivation.setCreatedAt(identityProviderActivationMongo.getCreatedAt());

        return identityProviderActivation;
    }
}
