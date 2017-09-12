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
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.redis.management.internal.RoleRedisRepository;
import io.gravitee.repository.redis.management.model.RedisRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisRoleRepository implements RoleRepository {

    @Autowired
    private RoleRedisRepository roleRedisRepository;

    @Override
    public Optional<Role> findById(RoleScope scope, String name) throws TechnicalException {
        RedisRole redisRole = roleRedisRepository.find(convertId(scope, name));
        return Optional.ofNullable(convert(redisRole));
    }

    @Override
    public Role create(Role role) throws TechnicalException {
        RedisRole redisRole = roleRedisRepository.saveOrUpdate(convert(role));
        return convert(redisRole);
    }

    @Override
    public Role update(Role role) throws TechnicalException {
        if (role == null || role.getScope() == null || role.getName() == null) {
            throw new IllegalStateException("Role to update must not be null");
        }
        final RedisRole redisRole = roleRedisRepository.find(convertId(role.getScope(), role.getName()));

        if (redisRole == null) {
            throw new IllegalStateException(String.format("No role found with scope [%s] and name [%s]", role.getScope(), role.getName()));
        }

        RedisRole redisRoleUpdated = roleRedisRepository.saveOrUpdate(convert(role));
        return convert(redisRoleUpdated);
    }

    @Override
    public void delete(RoleScope scope, String name) throws TechnicalException {
        roleRedisRepository.delete(convertId(scope, name));
    }

    @Override
    public Set<Role> findByScope(RoleScope scope) throws TechnicalException {
        return roleRedisRepository.
                findByScope(scope.getId()).
                stream().
                map(this::convert).
                collect(Collectors.toSet());
    }

    @Override
    public Set<Role> findAll() throws TechnicalException {
        return roleRedisRepository.
                findAll().
                stream().
                map(this::convert).
                collect(Collectors.toSet());
    }

    private Role convert(RedisRole redisRole) {
        if (redisRole == null) {
            return null;
        }

        Role role = new Role();
        role.setScope(RoleScope.valueOf(redisRole.getScope()));
        role.setName(redisRole.getName());
        role.setDescription(redisRole.getDescription());
        role.setDefaultRole(redisRole.isDefaultRole());
        role.setSystem(redisRole.isSystem());
        role.setPermissions(redisRole.getPermissions());
        if (redisRole.getUpdatedAt() > 0) {
            role.setUpdatedAt(new Date(redisRole.getUpdatedAt()));
        }
        if (redisRole.getCreatedAt() > 0) {
            role.setCreatedAt(new Date(redisRole.getCreatedAt()));
        }
        return role;
    }

    private String convertId(RoleScope scope, String name) {
        return scope.getId() + ':' + name;
    }

    private RedisRole convert(Role role) {
        if (role == null) {
            return null;
        }

        RedisRole redisRole = new RedisRole();
        redisRole.setId(convertId(role.getScope(), role.getName()));
        redisRole.setScope(role.getScope().getId());
        redisRole.setName(role.getName());
        redisRole.setDescription(role.getDescription());
        redisRole.setDefaultRole(role.isDefaultRole());
        redisRole.setSystem(role.isSystem());
        redisRole.setPermissions(role.getPermissions());
        if (role.getUpdatedAt() != null) {
            redisRole.setUpdatedAt(role.getUpdatedAt().getTime());
        }
        if (role.getCreatedAt() != null) {
            redisRole.setCreatedAt(role.getCreatedAt().getTime());
        }
        return redisRole;
    }
}
