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

import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.configuration.identity.*;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.impl.configuration.identity.IdentityProviderNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

@Component
public class IdentityProviderUpgrader implements Upgrader, Ordered {

    private static final String description =
        "Configuration provided by the system. Every modifications will be overridden at the next startup.";
    private final Logger logger = LoggerFactory.getLogger(IdentityProviderUpgrader.class);
    private List<String> notStorableIDPs = Arrays.asList("gravitee", "ldap", "memory");
    private List<String> idpTypeNames = Arrays.stream(IdentityProviderType.values()).map(Enum::name).collect(Collectors.toList());

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private GroupService groupService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private IdentityProviderActivationService identityProviderActivationService;

    @Override
    public boolean upgrade() {
        // FIXME : this upgrader uses the default ExecutionContext, but should handle all environments/organizations
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        boolean found = true;
        int idx = 0;

        while (found) {
            String type = environment.getProperty("security.providers[" + idx + "].type");
            found = (type != null);
            if (found && !notStorableIDPs.contains(type)) {
                if (idpTypeNames.contains(type.toUpperCase())) {
                    logger.info("Upsert identity provider config [{}]", type);
                    String id = environment.getProperty("security.providers[" + idx + "].id");
                    if (id == null) {
                        id = type;
                    }
                    try {
                        identityProviderService.findById(id);
                    } catch (IdentityProviderNotFoundException e) {
                        id = createIdp(executionContext, id, IdentityProviderType.valueOf(type.toUpperCase()), idx);
                    }
                    // always update
                    updateIdp(executionContext, id, idx);

                    // update idp activations
                    updateIdpActivations(executionContext, id, idx);
                } else {
                    logger.info("Unknown identity provider [{}]", type);
                }
            }
            idx++;
        }
        return true;
    }

    private String createIdp(ExecutionContext executionContext, String id, IdentityProviderType type, int providerIndex) {
        NewIdentityProviderEntity idp = new NewIdentityProviderEntity();
        idp.setName(id);
        idp.setType(type);
        idp.setDescription(description);
        idp.setEnabled(true);
        idp.setConfiguration(getConfiguration(providerIndex));
        idp.setEmailRequired(Boolean.valueOf((String) idp.getConfiguration().getOrDefault("emailRequired", "false")));
        idp.setSyncMappings(Boolean.valueOf((String) idp.getConfiguration().getOrDefault("syncMappings", "false")));

        Map<String, String> userProfileMapping = getUserProfileMapping(providerIndex);
        if (!userProfileMapping.isEmpty()) {
            idp.setUserProfileMapping(userProfileMapping);
        }

        return identityProviderService.create(executionContext, idp).getId();
    }

    private void updateIdpActivations(ExecutionContext executionContext, String id, int providerIndex) {
        //remove all previous activations
        identityProviderActivationService.deactivateIdpOnAllTargets(executionContext, id);

        ActivationTarget[] targets = getActivationsTarget(providerIndex);
        if (targets.length > 0) {
            identityProviderActivationService.activateIdpOnTargets(executionContext, id, targets);
        }
    }

    private ActivationTarget[] getActivationsTarget(int providerIndex) {
        List<String> targetStrings = getListOfString("security.providers[" + providerIndex + "].activations");
        List<ActivationTarget> activationTargets = new ArrayList<>();
        targetStrings.forEach(
            target -> {
                final String[] orgEnv = target.split(":");
                if (orgEnv.length == 1) {
                    try {
                        this.organizationService.findById(orgEnv[0]);
                        activationTargets.add(new ActivationTarget(orgEnv[0], IdentityProviderActivationReferenceType.ORGANIZATION));
                    } catch (OrganizationNotFoundException onfe) {
                        logger.warn("Organization {} does not exist", orgEnv[0]);
                    }
                } else if (orgEnv.length == 2) {
                    try {
                        this.organizationService.findById(orgEnv[0]);
                        EnvironmentEntity env = this.environmentService.findById(orgEnv[1]);
                        if (env.getOrganizationId().equals(orgEnv[0])) {
                            activationTargets.add(new ActivationTarget(orgEnv[1], IdentityProviderActivationReferenceType.ENVIRONMENT));
                        } else {
                            logger.warn("Environment {} does not exist in organization {}", orgEnv[1], orgEnv[0]);
                        }
                    } catch (OrganizationNotFoundException onfe) {
                        logger.warn("Organization {} does not exist", orgEnv[0]);
                    } catch (EnvironmentNotFoundException Enfe) {
                        logger.warn("Environment {} does not exist", orgEnv[1]);
                    }
                }
            }
        );
        return activationTargets.toArray(new ActivationTarget[activationTargets.size()]);
    }

    private void updateIdp(ExecutionContext executionContext, String id, int providerIndex) {
        UpdateIdentityProviderEntity idp = new UpdateIdentityProviderEntity();
        idp.setName(id);
        idp.setDescription(description);
        idp.setConfiguration(getConfiguration(providerIndex));
        idp.setEmailRequired(Boolean.valueOf((String) idp.getConfiguration().getOrDefault("emailRequired", "false")));
        idp.setEnabled(true);
        idp.setSyncMappings(Boolean.valueOf((String) idp.getConfiguration().getOrDefault("syncMappings", "false")));

        Map<String, String> userProfileMapping = getUserProfileMapping(providerIndex);
        if (!userProfileMapping.isEmpty()) {
            idp.setUserProfileMapping(userProfileMapping);
        }

        List<GroupMappingEntity> groupMappings = getGroupMappings(executionContext, providerIndex);
        if (!groupMappings.isEmpty()) {
            idp.setGroupMappings(groupMappings);
        }

        List<RoleMappingEntity> roleMappings = getRoleMappings(providerIndex);
        if (!roleMappings.isEmpty()) {
            idp.setRoleMappings(roleMappings);
        }

        identityProviderService.update(executionContext, id, idp);
    }

    private Map<String, Object> getConfiguration(int providerIndex) {
        HashMap<String, Object> config = new HashMap<>();

        String prefix = "security.providers[" + providerIndex + "].";
        putIfNotNull(config, prefix, "clientId");
        putIfNotNull(config, prefix, "clientSecret");
        putIfNotNull(config, prefix, "color");
        putIfNotNull(config, prefix, "tokenEndpoint");
        putIfNotNull(config, prefix, "authorizeEndpoint");
        putIfNotNull(config, prefix, "tokenIntrospectionEndpoint");
        putIfNotNull(config, prefix, "userInfoEndpoint");
        putIfNotNull(config, prefix, "userLogoutEndpoint");
        putIfNotNull(config, prefix, "serverURL");
        putIfNotNull(config, prefix, "domain");
        putIfNotNull(config, prefix, "emailRequired");
        putIfNotNull(config, prefix, "syncMappings");

        List<String> scopes = getListOfString("security.providers[" + providerIndex + "].scopes");
        if (!scopes.isEmpty()) {
            config.put("scopes", scopes);
        }

        return config;
    }

    private List<String> getListOfString(String listName) {
        boolean found = true;
        int idx = 0;
        ArrayList<String> scopes = new ArrayList<>();

        while (found) {
            String scope = environment.getProperty(listName + "[" + idx + "]");
            found = (scope != null);
            if (found) {
                scopes.add(scope);
            }
            idx++;
        }
        return scopes;
    }

    private Map<String, String> getUserProfileMapping(int providerIndex) {
        HashMap<String, String> mapping = new HashMap<>();

        String prefix = "security.providers[" + providerIndex + "].userMapping.";
        putIfNotNull(mapping, prefix, "id");
        putIfNotNull(mapping, prefix, "email");
        putIfNotNull(mapping, prefix, "lastname");
        putIfNotNull(mapping, prefix, "firstname");
        putIfNotNull(mapping, prefix, "picture");

        return mapping;
    }

    private List<GroupMappingEntity> getGroupMappings(ExecutionContext executionContext, int providerIndex) {
        boolean found = true;
        int idx = 0;
        List<GroupMappingEntity> mapping = new ArrayList<>();

        while (found) {
            String condition = environment.getProperty("security.providers[" + providerIndex + "].groupMapping[" + idx + "].condition");
            found = (condition != null);
            if (found) {
                GroupMappingEntity groupMappingEntity = new GroupMappingEntity();
                groupMappingEntity.setCondition(condition);
                List<String> groupNames = getListOfString("security.providers[" + providerIndex + "].groupMapping[" + idx + "].groups");
                if (!groupNames.isEmpty()) {
                    List<String> groups = new ArrayList<>();
                    groupNames.forEach(
                        groupName -> {
                            List<GroupEntity> groupsFound = groupService.findByName(executionContext.getEnvironmentId(), groupName);

                            if (groupsFound != null && groupsFound.size() == 1) {
                                groups.add(groupsFound.get(0).getId());
                            }
                        }
                    );

                    groupMappingEntity.setGroups(groups);
                }
                mapping.add(groupMappingEntity);
            }
            idx++;
        }

        return mapping;
    }

    private List<RoleMappingEntity> getRoleMappings(int providerIndex) {
        boolean found = true;
        int idx = 0;
        List<RoleMappingEntity> mapping = new ArrayList<>();

        while (found) {
            String condition = environment.getProperty("security.providers[" + providerIndex + "].roleMapping[" + idx + "].condition");
            found = (condition != null);
            if (found) {
                List<String> roles = getListOfString("security.providers[" + providerIndex + "].roleMapping[" + idx + "].roles");
                RoleMappingEntity roleMappingEntity = identityProviderService.getRoleMappings(condition, roles);
                mapping.add(roleMappingEntity);
            }
            idx++;
        }

        return mapping;
    }

    private void putIfNotNull(Map config, String prefix, String key) {
        String value = environment.getProperty(prefix + key);
        if (value != null) {
            config.put(key, value);
        }
    }

    @Override
    public int getOrder() {
        return 350;
    }
}
