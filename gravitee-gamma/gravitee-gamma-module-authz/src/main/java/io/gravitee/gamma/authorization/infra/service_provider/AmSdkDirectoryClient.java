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
package io.gravitee.gamma.authorization.infra.service_provider;

import io.gravitee.am.sdk.management.model.ApplicationPage;
import io.gravitee.am.sdk.management.model.FilteredApplication;
import io.gravitee.am.sdk.management.model.FilteredIdentityProviderInfo;
import io.gravitee.am.sdk.management.model.Group;
import io.gravitee.am.sdk.management.model.GroupPage;
import io.gravitee.am.sdk.management.model.Role;
import io.gravitee.am.sdk.management.model.RolePage;
import io.gravitee.am.sdk.management.model.User;
import io.gravitee.am.sdk.management.model.UserPage;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.core.am.model.AmAgent;
import io.gravitee.gamma.authorization.core.am.model.AmAgentPage;
import io.gravitee.gamma.authorization.core.am.model.AmGroup;
import io.gravitee.gamma.authorization.core.am.model.AmGroupMembersPage;
import io.gravitee.gamma.authorization.core.am.model.AmGroupPage;
import io.gravitee.gamma.authorization.core.am.model.AmRole;
import io.gravitee.gamma.authorization.core.am.model.AmRolePage;
import io.gravitee.gamma.authorization.core.am.model.AmUser;
import io.gravitee.gamma.authorization.core.am.model.AmUserPage;
import io.gravitee.gamma.authorization.core.am.service_provider.AmDirectoryClient;
import io.gravitee.gamma.authorization.infra.service_provider.AmSdkDirectoryClientFactory.AmSdkApis;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AM SDK-backed {@link AmDirectoryClient}. Owns the SDK specifics — client construction, the
 * well-known default AM org/env scoping, and mapping the SDK models onto the SDK-free core models.
 */
public class AmSdkDirectoryClient implements AmDirectoryClient {

    // AM resolves the domain by id alone; org/env only scope the service-account permission check,
    // so the well-known default AM org/env are used (see AM UsersResource#list).
    private static final String AM_DEFAULT_ORGANIZATION = "DEFAULT";
    private static final String AM_DEFAULT_ENVIRONMENT = "DEFAULT";

    // Agents are AM applications of type AGENT. The list endpoint omits clientId unless asked to
    // expand it; we need clientId to derive the entity id, so always request the expand (AM-6979).
    private static final String AGENT_APPLICATION_TYPE = "AGENT";
    private static final String CLIENT_ID_EXPAND = "clientId";

    private final AmSdkDirectoryClientFactory clientFactory;

    public AmSdkDirectoryClient(AmSdkDirectoryClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Session openSession(AmConnection connection) {
        // One SDK client (and its Vert.x WebClient connection pool) for the whole run, closed below.
        return new SdkSession(clientFactory.create(connection), connection.defaultDomainId());
    }

    private static final class SdkSession implements Session {

        private final AmSdkApis apis;
        private final String domainId;

        // IdP display name -> id, lazily loaded once per session. Reverses AM's management API,
        // which overwrites a user's `source` (the IdP id) with the IdP name on the way out.
        private Map<String, String> idpIdByName;

        private SdkSession(AmSdkApis apis, String domainId) {
            this.apis = apis;
            this.domainId = domainId;
        }

        @Override
        public AmUserPage fetchUsers(int page, int size) {
            UserPage userPage = AmSdkInvocations.await(
                apis.userApi().listUsers(AM_DEFAULT_ORGANIZATION, AM_DEFAULT_ENVIRONMENT, domainId, null, null, page, size)
            );
            Map<String, String> idpIds = idpIdByName();
            List<AmUser> users = (userPage.getData() == null ? List.<User>of() : userPage.getData())
                .stream()
                .map(user -> toAmUser(user, idpIds))
                .toList();
            long totalCount = userPage.getTotalCount() == null ? 0 : userPage.getTotalCount();
            return new AmUserPage(users, totalCount);
        }

        // The user `sub` AM issues hashes the IdP *id*, but listUsers returns the IdP *name* in
        // `source`; map name -> id so the synced PRINCIPAL entity is keyed on the same value.
        private Map<String, String> idpIdByName() {
            if (idpIdByName == null) {
                List<FilteredIdentityProviderInfo> idps = AmSdkInvocations.await(
                    apis.identityProviderApi().listIdentityProviders(AM_DEFAULT_ORGANIZATION, AM_DEFAULT_ENVIRONMENT, domainId, null)
                );
                idpIdByName = idps.stream()
                    .filter(idp -> idp.getName() != null && idp.getId() != null)
                    .collect(Collectors.toMap(FilteredIdentityProviderInfo::getName, FilteredIdentityProviderInfo::getId, (first, ignored) -> first));
            }
            return idpIdByName;
        }

        @Override
        public AmGroupPage fetchGroups(int page, int size) {
            GroupPage groupPage = AmSdkInvocations.await(
                apis.groupApi().listDomainGroups(AM_DEFAULT_ORGANIZATION, AM_DEFAULT_ENVIRONMENT, domainId, page, size)
            );
            List<AmGroup> groups = (groupPage.getData() == null ? List.<Group>of() : groupPage.getData())
                .stream()
                .map(AmSdkDirectoryClient::toAmGroup)
                .toList();
            long totalCount = groupPage.getTotalCount() == null ? 0 : groupPage.getTotalCount();
            return new AmGroupPage(groups, totalCount);
        }

        @Override
        public AmGroupMembersPage fetchGroupMembers(String groupId, int page, int size) {
            UserPage memberPage = AmSdkInvocations.await(
                apis.groupApi().getGroupMembers(AM_DEFAULT_ORGANIZATION, AM_DEFAULT_ENVIRONMENT, domainId, groupId, page, size)
            );
            List<String> memberUserIds = (memberPage.getData() == null ? List.<User>of() : memberPage.getData())
                .stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .toList();
            long totalCount = memberPage.getTotalCount() == null ? 0 : memberPage.getTotalCount();
            return new AmGroupMembersPage(memberUserIds, totalCount);
        }

        @Override
        public AmRolePage fetchRoles(int page, int size) {
            RolePage rolePage = AmSdkInvocations.await(
                apis.roleApi().findRoles(AM_DEFAULT_ORGANIZATION, AM_DEFAULT_ENVIRONMENT, domainId, page, size, null)
            );
            List<AmRole> roles = (rolePage.getData() == null ? List.<Role>of() : rolePage.getData())
                .stream()
                .map(AmSdkDirectoryClient::toAmRole)
                .toList();
            long totalCount = rolePage.getTotalCount() == null ? 0 : rolePage.getTotalCount();
            return new AmRolePage(roles, totalCount);
        }

        @Override
        public AmAgentPage fetchAgents(int page, int size) {
            // Filter to AGENT applications and expand clientId so each FilteredApplication carries the
            // client_id the entity id is derived from. Positional params follow the AM resource:
            // (org, env, domain, page, size, q, expand, status, owner.email, type).
            ApplicationPage applicationPage = AmSdkInvocations.await(
                apis
                    .applicationApi()
                    .listApplications(
                        AM_DEFAULT_ORGANIZATION,
                        AM_DEFAULT_ENVIRONMENT,
                        domainId,
                        page,
                        size,
                        null,
                        List.of(CLIENT_ID_EXPAND),
                        null,
                        null,
                        List.of(AGENT_APPLICATION_TYPE)
                    )
            );
            List<AmAgent> agents = (applicationPage.getData() == null ? List.<FilteredApplication>of() : applicationPage.getData())
                .stream()
                .map(AmSdkDirectoryClient::toAmAgent)
                .toList();
            long totalCount = applicationPage.getTotalCount() == null ? 0 : applicationPage.getTotalCount();
            return new AmAgentPage(agents, totalCount);
        }

        @Override
        public void close() {
            // Release the underlying Vert.x WebClient pool shared by all the API facades.
            apis.apiClient().getWebClient().close();
        }
    }

    static AmUser toAmUser(User user, Map<String, String> idpIdByName) {
        // listUsers returns the IdP name in `source`; resolve it back to the IdP id so the entity
        // matches AM's token `sub`. Fall back to the raw value when the IdP can't be resolved.
        String source = user.getSource();
        String sourceId = source == null ? null : idpIdByName.getOrDefault(source, source);
        return new AmUser(
            user.getId(),
            sourceId,
            user.getExternalId(),
            user.getEmail(),
            user.getUsername(),
            user.getDisplayName(),
            user.getEnabled(),
            user.getGroups(),
            user.getRoles()
        );
    }

    static AmAgent toAmAgent(FilteredApplication application) {
        // kind is the agent sub-type (USER_EMBEDDED / HOSTED_DELEGATED / AUTONOMOUS); clientId is
        // populated because the list call requests the clientId expand.
        String agentType = application.getKind() == null ? null : application.getKind().getValue();
        return new AmAgent(application.getId(), application.getClientId(), application.getName(), agentType);
    }

    static AmGroup toAmGroup(Group group) {
        return new AmGroup(group.getId(), group.getName());
    }

    static AmRole toAmRole(Role role) {
        return new AmRole(role.getId(), role.getName());
    }
}
