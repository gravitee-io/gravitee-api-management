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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.mongodb.management.internal.model.UserMongo;
import io.gravitee.repository.mongodb.management.internal.user.UserMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoUserRepository implements UserRepository {

    private final Pattern escaper = Pattern.compile("([^a-zA-Z0-9])");

    private final UserMongoRepository internalUserRepo;
    private final GraviteeMapper mapper;

    @Value("${management.mongodb.encryption.enabled:false}")
    private boolean isEncryptionEnabled;

    public MongoUserRepository(UserMongoRepository internalUserRepo, GraviteeMapper mapper) {
        this.internalUserRepo = internalUserRepo;
        this.mapper = mapper;
    }

    /**
     * Maps a domain user to its Mongo document, Base64-encoding the {@code idpClaims} keys. IdP claim names are
     * externally controlled and OIDC namespaced claims contain dots, which MongoDB rejects as map keys; encoding
     * the keys mirrors how {@code MongoIdentityProviderRepository} stores its group/role mapping keys.
     */
    private UserMongo toMongo(User user) {
        UserMongo userMongo = mapper.map(user);
        if (userMongo != null) {
            userMongo.setIdpClaims(encodeClaimKeys(user.getIdpClaims()));
        }
        return userMongo;
    }

    private User toModel(UserMongo userMongo) {
        User user = mapper.map(userMongo);
        if (user != null) {
            try {
                user.setIdpClaims(decodeClaimKeys(userMongo.getIdpClaims()));
            } catch (IllegalArgumentException e) {
                // A non-Base64 key (e.g. from an out-of-band edit/import) must not make the user unreadable.
                log.warn("Failed to decode idp_claims keys for user {}; ignoring stored claims", userMongo.getId(), e);
                user.setIdpClaims(null);
            }
        }
        return user;
    }

    private static Map<String, String> encodeClaimKeys(Map<String, String> claims) {
        if (claims == null) {
            return null;
        }
        Map<String, String> encoded = new HashMap<>(claims.size());
        claims.forEach((key, value) -> encoded.put(new String(Base64.getEncoder().encode(key.getBytes())), value));
        return encoded;
    }

    private static Map<String, String> decodeClaimKeys(Map<String, String> claims) {
        if (claims == null) {
            return null;
        }
        Map<String, String> decoded = new HashMap<>(claims.size());
        claims.forEach((key, value) -> decoded.put(new String(Base64.getDecoder().decode(key)), value));
        return decoded;
    }

    @Override
    public Optional<User> findBySource(String source, String sourceId, String organizationId) {
        log.debug("Find user by name source[{}] user[{}]", source, sourceId);

        if (sourceId == null) {
            return Optional.empty();
        }

        UserMongo user;
        if (isEncryptionEnabled) {
            user = internalUserRepo.findBySourceAndSourceId(source, sourceId.toLowerCase(), organizationId);
        } else {
            String escapedSourceId = escaper.matcher(sourceId).replaceAll("\\\\$1");
            user = internalUserRepo.findBySourceAndSourceIdIgnoreCase(source, escapedSourceId, organizationId);
        }
        User res = toModel(user);

        return Optional.ofNullable(res);
    }

    @Override
    public List<User> findByEmail(String email, String organizationId) {
        log.debug("Find user by email [{}]", email);

        if (email == null) {
            return List.of();
        }

        List<UserMongo> users;
        if (isEncryptionEnabled) {
            users = internalUserRepo.findByEmail(email.toLowerCase(), organizationId);
        } else {
            users = internalUserRepo.findByEmailIgnoreCase(email, organizationId);
        }

        return users.stream().map(this::toModel).toList();
    }

    @Override
    public Set<User> findByIds(Collection<String> ids) {
        log.debug("Find user by identifiers user [{}]", ids);

        Set<UserMongo> usersMongo = internalUserRepo.findByIds(ids);
        Set<User> users = usersMongo.stream().map(this::toModel).collect(Collectors.toSet());

        log.debug("Find user by identifiers user [{}] - Done", ids);
        return users;
    }

    @Override
    public Page<User> search(UserCriteria criteria, Pageable pageable) throws TechnicalException {
        log.debug("search users");

        var users = internalUserRepo.search(criteria, pageable).map(this::toModel);

        log.debug("search users - Done");
        return users;
    }

    @Override
    public List<String> deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("Delete users by organizationId: {}", organizationId);
        try {
            final var users = internalUserRepo.deleteByOrganizationId(organizationId).stream().map(UserMongo::getId).toList();
            log.debug("Delete users by organizationId: {} - Done", organizationId);
            return users;
        } catch (Exception ex) {
            log.error("Failed to delete users by organizationId: {}", organizationId, ex);
            throw new TechnicalException("Failed to delete users by organizationId");
        }
    }

    @Override
    public Optional<User> findById(String id) throws TechnicalException {
        log.debug("Find user by ID [{}]", id);

        UserMongo user = internalUserRepo.findById(id).orElse(null);
        User res = toModel(user);

        log.debug("Find user by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public User create(User user) throws TechnicalException {
        log.debug("Create user [{}]", user.getId());
        try {
            UserMongo userMongo = toMongo(user);

            if (isEncryptionEnabled) {
                if (userMongo.getSourceId() != null) {
                    userMongo.setSourceId(userMongo.getSourceId().toLowerCase());
                }
                if (userMongo.getEmail() != null) {
                    userMongo.setEmail(userMongo.getEmail().toLowerCase());
                }
            }

            UserMongo createdUserMongo = internalUserRepo.insert(userMongo);

            User res = toModel(createdUserMongo);

            log.debug("Create user [{}] - Done", user.getId());

            return res;
        } catch (Exception ex) {
            log.error("Failed to create user with id: {}", user.getId(), ex);
            throw new TechnicalException("Failed to create user");
        }
    }

    @Override
    public User update(User user) throws TechnicalException {
        if (user == null || user.getId() == null) {
            throw new IllegalStateException("User to update must have an identifier");
        }

        final UserMongo userMongo = internalUserRepo.findById(user.getId()).orElse(null);

        if (userMongo == null) {
            throw new IllegalStateException(String.format("No user found with username [%s]", user.getId()));
        }

        userMongo.setSource(user.getSource());
        userMongo.setOrganizationId(user.getOrganizationId());
        userMongo.setSourceId(user.getSourceId());
        userMongo.setFirstname(user.getFirstname());
        userMongo.setLastname(user.getLastname());
        userMongo.setCreatedAt(user.getCreatedAt());
        userMongo.setUpdatedAt(user.getUpdatedAt());
        userMongo.setPassword(user.getPassword());
        userMongo.setPicture(user.getPicture());
        userMongo.setEmail(user.getEmail());
        if (user.getStatus() != null) {
            userMongo.setStatus(user.getStatus().name());
        }
        userMongo.setLastConnectionAt(user.getLastConnectionAt());
        userMongo.setLoginCount(user.getLoginCount());
        userMongo.setFirstConnectionAt(user.getFirstConnectionAt());
        userMongo.setNewsletterSubscribed(user.getNewsletterSubscribed());
        userMongo.setIsServiceAccount(user.getIsServiceAccount());
        userMongo.setIdpClaims(encodeClaimKeys(user.getIdpClaims()));
        UserMongo userUpdated = internalUserRepo.save(userMongo);
        return toModel(userUpdated);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete user [{}]", id);
        internalUserRepo.deleteById(id);
    }

    @Override
    public Set<User> findAll() throws TechnicalException {
        return internalUserRepo.findAll().stream().map(this::toModel).collect(Collectors.toSet());
    }
}
