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
package io.gravitee.rest.api.services.v3.upgrader;

import io.gravitee.common.service.AbstractService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class V3UpgraderService extends AbstractService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(V3UpgraderService.class);

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Value("${services.v3-upgrader.enabled:true}")
    private boolean enabled;

    @Override
    protected String name() {
        return "V3 Upgrader Service";
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            logger.info("v3 Upgrader service is enabled");
            convertIDPRoleMapping();
            moveIdpPermission();
        } else {
            logger.info("v3 Upgrader service has been disabled");
        }
    }

    public void convertIDPRoleMapping() {
        try {
            Set<IdentityProvider> allIdps = identityProviderRepository.findAll();
            logger.debug("{} Idp found", allIdps.size());
            for (IdentityProvider idp : allIdps) {
                boolean idpHasBeenModified = false;
                Map<String, String[]> roleMappings = idp.getRoleMappings();
                if (roleMappings != null) {
                    logger.debug("Idp '{}' has {} roleMappings", idp.getId(), roleMappings.size());
                    for (Entry<String, String[]> entry : roleMappings.entrySet()) {
                        String[] roles = entry.getValue();
                        if (roles != null) {
                            logger.debug("Idp '{}' - RoleMapping with condition '{}' has {} roles", idp.getId(), entry.getKey(), roles.length);
                            List<String> newRoles = new ArrayList<>(roles.length);
                            boolean entryHasBeenModified = false;
                            for (String role : roles) {
                                String[] splittedRole = role.split(":");
                                if ("1".equals(splittedRole[0])) {
                                    Optional<Role> existingOrgaRoleWithSameName = roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.ORGANIZATION, splittedRole[1], "DEFAULT", RoleReferenceType.ORGANIZATION);
                                    if (existingOrgaRoleWithSameName.isPresent()) {
                                        newRoles.add("ORGANIZATION:" + splittedRole[1]);
                                    }
                                    newRoles.add("ENVIRONMENT:" + splittedRole[1]);
                                    entryHasBeenModified = true;
                                } else if ("2".equals(splittedRole[0])) {
                                    newRoles.add("ENVIRONMENT:" + splittedRole[1]);
                                    entryHasBeenModified = true;
                                }
                            }

                            if (entryHasBeenModified) {
                                entry.setValue(newRoles.toArray(new String[newRoles.size()]));
                                idpHasBeenModified = true;
                            }
                        } else {
                            logger.debug("Idp '{}' - RoleMapping with condition '{}' has no roles", idp.getId(), entry.getKey());
                        }
                    }
                }
                if (idpHasBeenModified) {
                    identityProviderRepository.update(idp);
                    logger.info("Idp '{}' has been updated", idp.getId());
                } else {
                    logger.info("Idp '{}' has not been updated", idp.getId());
                }
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to retrieve all identity providers", ex);
            throw new TechnicalManagementException(
                    "An error occurs while trying to retrieve identity providers", ex);
        }
    }

    public void moveIdpPermission() {
        try {
            final Set<Role> allRole = this.roleRepository.findByScopeAndReferenceIdAndReferenceType(RoleScope.ENVIRONMENT, "DEFAULT", RoleReferenceType.ORGANIZATION);
            logger.info("{} environment roles found", allRole.size());
            for (Role envRole : allRole) {
                int idpPerm = -1;
                int idpPermIndex = -1;
                final int[] envPermissions = envRole.getPermissions();
                for (int index = 0; index < envPermissions.length; index++) {
                    if (envPermissions[index] > 3399 && envPermissions[index] < 3500) {
                        idpPerm = envPermissions[index];
                        idpPermIndex = index;
                    }
                }

                if (idpPerm != -1 && idpPermIndex != -1) {
                    logger.info("Permission: {} found on role {}", idpPerm, envRole.getName());

                    Role orgRole;
                    final Optional<Role> existingOrgRoleWithSameNameRoleCursor = this.roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.ORGANIZATION, envRole.getName(), envRole.getReferenceId(), envRole.getReferenceType());
                    if (existingOrgRoleWithSameNameRoleCursor.isPresent()) {
                        logger.info("An org role exist with the same name");
                        orgRole = existingOrgRoleWithSameNameRoleCursor.get();
                        orgRole.setPermissions(ArrayUtils.add(orgRole.getPermissions(), 1300 + idpPerm % 100));
                        logger.info("permissions updated");

                        this.roleRepository.update(orgRole);
                        logger.info("Update of org role done");

                    } else {
                        logger.info("No org role exist with the same name");

                        orgRole = new Role();
                        orgRole.setId(RandomString.generate());
                        orgRole.setName(envRole.getName());
                        orgRole.setDefaultRole(envRole.isDefaultRole());
                        orgRole.setReferenceId(GraviteeContext.getDefaultOrganization());
                        orgRole.setReferenceType(RoleReferenceType.ORGANIZATION);
                        orgRole.setScope(RoleScope.ORGANIZATION);
                        orgRole.setCreatedAt(new Date());
                        orgRole.setDescription(envRole.getDescription());
                        orgRole.setSystem(envRole.isSystem());
                        orgRole.setPermissions(new int[]{1300 + idpPerm % 100});
                        this.roleRepository.create(orgRole);
                        logger.info("Creation of org role done");

                    }

                    envRole.setPermissions(ArrayUtils.remove(envPermissions, idpPermIndex));
                    this.roleRepository.update(envRole);
                    logger.info("Remove permission from env role done");

                    // Update memberships
                    final Set<Membership> envMembershipsWithRole = this.membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.ENVIRONMENT, "DEFAULT", envRole.getId());
                    for (Membership membership : envMembershipsWithRole) {
                        Set<Membership> orgMembershipsWithRole = this.membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(membership.getMemberId(), membership.getMemberType(), MembershipReferenceType.ORGANIZATION, "DEFAULT", orgRole.getId());
                        if (orgMembershipsWithRole.isEmpty()) {
                            Membership newOrganizationMembership = membership;
                            newOrganizationMembership.setId(RandomString.generate());
                            newOrganizationMembership.setReferenceId("DEFAULT");
                            newOrganizationMembership.setReferenceType(MembershipReferenceType.ORGANIZATION);
                            newOrganizationMembership.setRoleId(orgRole.getId());

                            this.membershipRepository.create(newOrganizationMembership);
                            logger.info("New membership {} on organization created for user {}", newOrganizationMembership.getId(), membership.getMemberId());
                        }
                    }
                }
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to retrieve all identity providers", ex);
            throw new TechnicalManagementException(
                    "An error occurs while trying to retrieve identity providers", ex);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

}
