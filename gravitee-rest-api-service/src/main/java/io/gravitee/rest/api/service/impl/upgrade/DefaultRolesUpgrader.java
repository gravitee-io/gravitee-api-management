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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.model.permissions.ManagementPermission;
import io.gravitee.rest.api.model.permissions.PortalPermission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.model.permissions.RoleScope.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultRolesUpgrader implements Upgrader, Ordered {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultRolesUpgrader.class);

    @Autowired
    private RoleService roleService;

    @Override
    public boolean upgrade() {
        // initialize roles.
        if (roleService.findAll().isEmpty()) {
            logger.info("    No role found. Add default ones.");

            Map<String, char[]> perms = new HashMap<>();

            logger.info("     - <MANAGEMENT> API_PUBLISHER");
            perms.put(ManagementPermission.API.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ManagementPermission.APPLICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ManagementPermission.TAG.getName(), new char[]{READ.getId()});
            perms.put(ManagementPermission.GROUP.getName(), new char[]{READ.getId()});
            perms.put(ManagementPermission.TENANT.getName(), new char[]{READ.getId()});
            perms.put(ManagementPermission.ROLE.getName(), new char[]{READ.getId()});
            perms.put(ManagementPermission.PLATFORM.getName(), new char[]{READ.getId()});
            roleService.create(new NewRoleEntity(
                    "API_PUBLISHER",
                    "Management Role. Created by Gravitee.io.",
                    MANAGEMENT,
                    false,
                    perms
            ));

            logger.info("     - <MANAGEMENT> USER (default)");
            perms.clear();
            perms.put(ManagementPermission.API.getName(), new char[]{READ.getId()});
            perms.put(ManagementPermission.APPLICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ManagementPermission.ROLE.getName(), new char[]{READ.getId()});
            perms.put(ManagementPermission.GROUP.getName(), new char[]{READ.getId()});
            roleService.create(new NewRoleEntity(
                    "USER",
                    "Default Management Role. Created by Gravitee.io.",
                    MANAGEMENT,
                    true,
                    perms
            ));

            logger.info("     - <PORTAL> USER (default)");
            perms.clear();
            perms.put(PortalPermission.APPLICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(PortalPermission.DOCUMENTATION.getName(), new char[]{READ.getId()});
            roleService.create(new NewRoleEntity(
                    "USER",
                    "Default Portal Role. Created by Gravitee.io.",
                    PORTAL,
                    true,
                    perms
            ));

            logger.info("     - <API> USER (default)");
            perms.clear();
            perms.put(ApiPermission.DEFINITION.getName(), new char[]{READ.getId()});
            perms.put(ApiPermission.PLAN.getName(), new char[]{READ.getId()});
            perms.put(ApiPermission.SUBSCRIPTION.getName(), new char[]{READ.getId()});
            perms.put(ApiPermission.MEMBER.getName(), new char[]{READ.getId()});
            perms.put(ApiPermission.METADATA.getName(), new char[]{READ.getId()});
            perms.put(ApiPermission.EVENT.getName(), new char[]{READ.getId()});
            perms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{READ.getId()});
            perms.put(ApiPermission.RATING.getName(), new char[]{CREATE.getId(), READ.getId()});
            roleService.create(new NewRoleEntity(
                    "USER",
                    "Default API Role. Created by Gravitee.io.",
                    API,
                    true,
                    perms
            ));

            logger.info("     - <API> OWNER");
            perms.clear();
            perms.put(ApiPermission.DEFINITION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.GATEWAY_DEFINITION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.PLAN.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.SUBSCRIPTION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.MEMBER.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.METADATA.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.ANALYTICS.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.EVENT.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.HEALTH.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.LOG.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.AUDIT.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.RATING.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.RATING_ANSWER.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.DISCOVERY.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.NOTIFICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApiPermission.ALERT.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            roleService.create(new NewRoleEntity(
                    "OWNER",
                    "API Role. Created by Gravitee.io.",
                    API,
                    false,
                    perms
            ));

            createRoleApiReviewer(perms);

            logger.info("     - <APPLICATION> USER (default)");
            perms.clear();
            perms.put(ApplicationPermission.DEFINITION.getName(), new char[]{READ.getId()});
            perms.put(ApplicationPermission.MEMBER.getName(), new char[]{READ.getId()});
            perms.put(ApplicationPermission.ANALYTICS.getName(), new char[]{READ.getId()});
            perms.put(ApplicationPermission.LOG.getName(), new char[]{READ.getId()});
            perms.put(ApplicationPermission.SUBSCRIPTION.getName(), new char[]{CREATE.getId(), READ.getId()});
            roleService.create(new NewRoleEntity(
                    "USER",
                    "Default Application Role. Created by Gravitee.io.",
                    APPLICATION,
                    true,
                    perms
            ));

            logger.info("     - <APPLICATION> OWNER");
            perms.clear();
            perms.put(ApplicationPermission.DEFINITION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApplicationPermission.MEMBER.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApplicationPermission.ANALYTICS.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApplicationPermission.LOG.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApplicationPermission.SUBSCRIPTION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApplicationPermission.NOTIFICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            perms.put(ApplicationPermission.ALERT.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            roleService.create(new NewRoleEntity(
                    "OWNER",
                    "Default Application Role. Created by Gravitee.io.",
                    APPLICATION,
                    false,
                    perms
            ));
        } else {
            try {
                roleService.findById(RoleScope.API, "REVIEWER");
            } catch (final RoleNotFoundException rnfe) {
                createRoleApiReviewer(new HashMap<>());
            }
        }
        roleService.createOrUpdateSystemRoles();

        return true;
    }

    private void createRoleApiReviewer(final Map<String, char[]> perms) {
        logger.info("     - <API> REVIEWER");
        perms.clear();
        perms.put(ApiPermission.DEFINITION.getName(), new char[]{READ.getId()});
        perms.put(ApiPermission.GATEWAY_DEFINITION.getName(), new char[]{READ.getId()});
        perms.put(ApiPermission.PLAN.getName(), new char[]{READ.getId()});
        perms.put(ApiPermission.METADATA.getName(), new char[]{READ.getId()});
        perms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{READ.getId()});
        perms.put(ApiPermission.DISCOVERY.getName(), new char[]{READ.getId()});
        perms.put(ApiPermission.NOTIFICATION.getName(), new char[]{READ.getId(), UPDATE.getId()});
        perms.put(ApiPermission.ALERT.getName(), new char[]{READ.getId()});
        perms.put(ApiPermission.REVIEWS.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
        roleService.create(new NewRoleEntity(
                "REVIEWER",
                "API Role. Created by Gravitee.io.",
                API,
                false,
                perms
        ));
    }

    @Override
    public int getOrder() {
        return 150;
    }
}
