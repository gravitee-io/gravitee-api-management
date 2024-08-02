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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoUserRepository implements UserRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Pattern escaper = Pattern.compile("([^a-zA-Z0-9])");

    @Autowired
    private UserMongoRepository internalUserRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<User> findBySource(String source, String sourceId, String organizationId) {
        logger.debug("Find user by name source[{}] user[{}]", source, sourceId);

        String escapedSourceId = escaper.matcher(sourceId).replaceAll("\\\\$1");
        UserMongo user = internalUserRepo.findBySourceAndSourceId(source, escapedSourceId, organizationId);
        User res = mapper.map(user);

        return Optional.ofNullable(res);
    }

    @Override
    public Optional<User> findByEmail(String email, String organizationId) {
        logger.debug("Find user by email [{}]", email);

        UserMongo user = internalUserRepo.findByEmail(email, organizationId);
        User res = mapper.map(user);

        return Optional.ofNullable(res);
    }

    @Override
    public Set<User> findByIds(Collection<String> ids) {
        logger.debug("Find user by identifiers user [{}]", ids);

        Set<UserMongo> usersMongo = internalUserRepo.findByIds(ids);
        Set<User> users = mapper.mapUsers(usersMongo);

        logger.debug("Find user by identifiers user [{}] - Done", ids);
        return users;
    }

    @Override
    public Page<User> search(UserCriteria criteria, Pageable pageable) throws TechnicalException {
        logger.debug("search users");

        Page<UserMongo> users = internalUserRepo.search(criteria, pageable);
        List<User> content = mapper.mapUserList(users.getContent());

        logger.debug("search users - Done");
        return new Page<>(content, users.getPageNumber(), (int) users.getPageElements(), users.getTotalElements());
    }

    @Override
    public List<String> deleteByOrganizationId(String organizationId) throws TechnicalException {
        logger.debug("Delete users by organizationId: {}", organizationId);
        try {
            final var users = internalUserRepo.deleteByOrganizationId(organizationId).stream().map(UserMongo::getId).toList();
            logger.debug("Delete users by organizationId: {} - Done", organizationId);
            return users;
        } catch (Exception ex) {
            logger.error("Failed to delete users by organizationId: {}", organizationId, ex);
            throw new TechnicalException("Failed to delete users by organizationId");
        }
    }

    @Override
    public Optional<User> findById(String id) throws TechnicalException {
        logger.debug("Find user by ID [{}]", id);

        UserMongo user = internalUserRepo.findById(id).orElse(null);
        User res = mapper.map(user);

        logger.debug("Find user by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public User create(User user) throws TechnicalException {
        logger.debug("Create user [{}]", user.getId());

        UserMongo userMongo = mapper.map(user);
        UserMongo createdUserMongo = internalUserRepo.insert(userMongo);

        User res = mapper.map(createdUserMongo);

        logger.debug("Create user [{}] - Done", user.getId());

        return res;
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
        UserMongo userUpdated = internalUserRepo.save(userMongo);
        return mapper.map(userUpdated);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("Delete user [{}]", id);
        internalUserRepo.deleteById(id);
    }

    @Override
    public Set<User> findAll() throws TechnicalException {
        return internalUserRepo.findAll().stream().map(userMongo -> mapper.map(userMongo)).collect(Collectors.toSet());
    }
}
