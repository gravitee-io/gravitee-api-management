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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.TOKEN;
import static io.gravitee.repository.management.model.Token.AuditEvent.*;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TokenRepository;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.model.TokenReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.TokenNameAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.TokenNotFoundException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TokenServiceImpl extends AbstractService implements TokenService {

    private final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Lazy
    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public List<TokenEntity> findByUser(final String userId) {
        try {
            LOGGER.debug("Find all tokens for user '{}'", userId);
            return tokenRepository.findByReference(TokenReferenceType.USER.name(), userId).stream().map(this::convert).collect(toList());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find all tokens";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public TokenEntity create(final ExecutionContext executionContext, NewTokenEntity newToken, String user) {
        try {
            // check if name already exists
            final List<TokenEntity> tokens = findByUser(user);
            final boolean nameAlreadyExists = tokens.stream().anyMatch(token -> newToken.getName().equalsIgnoreCase(token.getName()));
            if (nameAlreadyExists) {
                throw new TokenNameAlreadyExistsException(newToken.getName());
            }

            final String decodedToken = UUID.toString(UUID.random());
            final Token token = convert(newToken, TokenReferenceType.USER, user, passwordEncoder.encode(decodedToken));
            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(TOKEN, token.getId()),
                TOKEN_CREATED,
                token.getCreatedAt(),
                null,
                token
            );
            return convert(tokenRepository.create(token), decodedToken);
        } catch (TechnicalException e) {
            final String error = "An error occurs while trying to create a token " + newToken;
            LOGGER.error(error, e);
            throw new TechnicalManagementException(error, e);
        }
    }

    @Override
    public void revokeByUser(final ExecutionContext executionContext, String userId) {
        final List<TokenEntity> tokens = findByUser(userId);
        tokens.forEach(token -> revoke(executionContext, token.getId()));
    }

    @Override
    public void revoke(final ExecutionContext executionContext, String tokenId) {
        try {
            Optional<Token> tokenOptional = tokenRepository.findById(tokenId);
            if (tokenOptional.isPresent()) {
                tokenRepository.delete(tokenId);
                auditService.createOrganizationAuditLog(
                    executionContext,
                    executionContext.getOrganizationId(),
                    Collections.singletonMap(TOKEN, tokenId),
                    TOKEN_DELETED,
                    new Date(),
                    null,
                    tokenOptional.get()
                );
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to delete token " + tokenId;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public Token findByToken(String token) {
        try {
            LOGGER.debug("Find token entity by token value");

            Token matchingToken = tokenRepository
                .findAll()
                .stream()
                .sorted(comparing(Token::getLastUseAt, nullsLast(reverseOrder())))
                .filter(t -> passwordEncoder.matches(token, t.getToken()))
                .findFirst()
                .orElseThrow(() -> new TokenNotFoundException(token));

            matchingToken.setLastUseAt(new Date());
            return tokenRepository.update(matchingToken);
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find token entity for a given token value";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public boolean tokenExistsForUser(String tokenId, String userId) {
        try {
            Optional<Token> byId = tokenRepository.findById(tokenId);
            return byId
                .map(Token::getReferenceId)
                .filter(ref -> ref.equals(userId))
                .isPresent();
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to check if token exists";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private Token convert(
        final NewTokenEntity tokenEntity,
        final TokenReferenceType referenceType,
        final String referenceId,
        final String encodedToken
    ) {
        final Token token = new Token();
        token.setId(UUID.toString(UUID.random()));
        token.setToken(encodedToken);
        token.setName(tokenEntity.getName());
        token.setCreatedAt(new Date());
        token.setReferenceType(referenceType.name());
        token.setReferenceId(referenceId);
        return token;
    }

    private TokenEntity convert(final Token token) {
        return convert(token, null);
    }

    private TokenEntity convert(final Token token, final String decodedToken) {
        final TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setId(token.getId());
        tokenEntity.setToken(decodedToken);
        tokenEntity.setName(token.getName());
        tokenEntity.setCreatedAt(token.getCreatedAt());
        tokenEntity.setExpiresAt(token.getExpiresAt());
        tokenEntity.setLastUseAt(token.getLastUseAt());
        return tokenEntity;
    }
}
