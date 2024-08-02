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
import io.gravitee.repository.management.api.TokenRepository;
import io.gravitee.repository.management.model.Token;
import io.gravitee.repository.mongodb.management.internal.api.TokenMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.TokenMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoTokenRepository implements TokenRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoTokenRepository.class);

    @Autowired
    private TokenMongoRepository internalTokenRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Token> findById(String tokenId) throws TechnicalException {
        LOGGER.debug("Find token by ID [{}]", tokenId);
        final TokenMongo token = internalTokenRepo.findById(tokenId).orElse(null);
        LOGGER.debug("Find token by ID [{}] - Done", tokenId);
        return Optional.ofNullable(mapper.map(token));
    }

    @Override
    public Token create(Token token) throws TechnicalException {
        LOGGER.debug("Create token [{}]", token.getName());
        TokenMongo tokenMongo = mapper.map(token);
        TokenMongo createdTokenMongo = internalTokenRepo.insert(tokenMongo);
        Token res = mapper.map(createdTokenMongo);
        LOGGER.debug("Create token [{}] - Done", token.getName());
        return res;
    }

    @Override
    public Token update(Token token) throws TechnicalException {
        if (token == null || token.getName() == null) {
            throw new IllegalStateException("Token to update must have a name");
        }

        final TokenMongo tokenMongo = internalTokenRepo.findById(token.getId()).orElse(null);

        if (tokenMongo == null) {
            throw new IllegalStateException(String.format("No token found with name [%s]", token.getToken()));
        }
        try {
            tokenMongo.setToken(token.getToken());
            tokenMongo.setReferenceId(token.getReferenceId());
            tokenMongo.setReferenceType(token.getReferenceType());
            tokenMongo.setName(token.getName());
            tokenMongo.setCreatedAt(token.getCreatedAt());
            tokenMongo.setExpiresAt(token.getExpiresAt());
            tokenMongo.setLastUseAt(token.getLastUseAt());

            TokenMongo tokenMongoUpdated = internalTokenRepo.save(tokenMongo);
            return mapper.map(tokenMongoUpdated);
        } catch (Exception e) {
            final String error = "An error occurred when updating token";
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public void delete(String tokenId) throws TechnicalException {
        try {
            internalTokenRepo.deleteById(tokenId);
        } catch (Exception e) {
            final String error = "An error occurred when deleting token [" + tokenId + "]";
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public Set<Token> findAll() throws TechnicalException {
        final List<TokenMongo> tokens = internalTokenRepo.findAll();
        return tokens.stream().map(tokenMongo -> mapper.map(tokenMongo)).collect(Collectors.toSet());
    }

    @Override
    public List<Token> findByReference(String referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("Find token by ref type '{}' and ref id '{}'", referenceType, referenceId);
        final List<TokenMongo> token = internalTokenRepo.findByReferenceTypeAndReferenceId(referenceType, referenceId);
        LOGGER.debug("Find token by ref type '{}' and ref id '{}' done", referenceType, referenceId);
        return token.stream().map(t -> mapper.map(t)).collect(Collectors.toList());
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, String referenceType) throws TechnicalException {
        LOGGER.debug("Delete token by ref type '{}' and ref id '{}'", referenceType, referenceId);
        try {
            final var tokens = internalTokenRepo
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType)
                .stream()
                .map(TokenMongo::getId)
                .toList();
            LOGGER.debug("Delete token by ref type '{}' and ref id '{}' done", referenceId, referenceType);
            return tokens;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete tokens for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete tokens by reference", ex);
        }
    }
}
