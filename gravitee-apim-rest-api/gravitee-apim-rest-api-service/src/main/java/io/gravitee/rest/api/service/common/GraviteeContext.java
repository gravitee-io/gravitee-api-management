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
package io.gravitee.rest.api.service.common;

import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserMetadataEntity;
import io.gravitee.rest.api.model.parameters.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeContext {

    private static final String DEFAULT_ENVIRONMENT = "DEFAULT";
    private static final String DEFAULT_ORGANIZATION = "DEFAULT";

    private static final String CURRENT_ENVIRONMENT_CONTEXT_KEY = "currentEnvironment";
    private static final String CURRENT_ORGANIZATION_CONTEXT_KEY = "currentOrganization";
    private static final String ROLES_CONTEXT_CACHE_KEY = "currentRoles";
    private static final String USERS_CONTEXT_CACHE_KEY = "currentUsers";
    private static final String USERS_METADATA_CONTEXT_CACHE_KEY = "currentUsersMetadata";
    private static final String PARAMETERS_CONTEXT_CACHE_KEY = "currentParameters";

    private static final ThreadLocal<Map<String, Object>> contextThread = ThreadLocal.withInitial(
        () -> {
            Map<String, Object> propertiesMap = new HashMap<>();
            propertiesMap.put(CURRENT_ENVIRONMENT_CONTEXT_KEY, DEFAULT_ENVIRONMENT);
            propertiesMap.put(CURRENT_ORGANIZATION_CONTEXT_KEY, DEFAULT_ORGANIZATION);
            propertiesMap.put(ROLES_CONTEXT_CACHE_KEY, new ConcurrentHashMap<>());
            propertiesMap.put(USERS_CONTEXT_CACHE_KEY, new ConcurrentHashMap<>());
            propertiesMap.put(USERS_METADATA_CONTEXT_CACHE_KEY, new ConcurrentHashMap<>());
            propertiesMap.put(PARAMETERS_CONTEXT_CACHE_KEY, new ConcurrentHashMap<>());
            return propertiesMap;
        }
    );

    public static void cleanContext() {
        contextThread.remove();
    }

    public static String getCurrentEnvironment() {
        return (String) contextThread.get().get(CURRENT_ENVIRONMENT_CONTEXT_KEY);
    }

    public static String getCurrentEnvironmentOrDefault() {
        String currentEnvironment = getCurrentEnvironment();
        return StringUtils.isEmpty(currentEnvironment) ? getDefaultEnvironment() : currentEnvironment;
    }

    public static void setCurrentEnvironment(String currentEnvironment) {
        contextThread.get().put(CURRENT_ENVIRONMENT_CONTEXT_KEY, currentEnvironment);
    }

    public static String getDefaultEnvironment() {
        return DEFAULT_ENVIRONMENT;
    }

    public static String getCurrentOrganization() {
        return (String) contextThread.get().get(CURRENT_ORGANIZATION_CONTEXT_KEY);
    }

    public static void setCurrentOrganization(String currentOrganization) {
        contextThread.get().put(CURRENT_ORGANIZATION_CONTEXT_KEY, currentOrganization);
    }

    public static String getDefaultOrganization() {
        return DEFAULT_ORGANIZATION;
    }

    public static ConcurrentMap<Key, String> getCurrentParameters() {
        return (ConcurrentMap) contextThread.get().get(PARAMETERS_CONTEXT_CACHE_KEY);
    }

    public static ConcurrentMap<String, RoleEntity> getCurrentRoles() {
        return (ConcurrentMap) contextThread.get().get(ROLES_CONTEXT_CACHE_KEY);
    }

    public static ConcurrentMap<String, UserEntity> getCurrentUsers() {
        return (ConcurrentMap) contextThread.get().get(USERS_CONTEXT_CACHE_KEY);
    }

    public static ConcurrentMap<String, List<UserMetadataEntity>> getCurrentUsersMetadata() {
        return (ConcurrentMap) contextThread.get().get(USERS_METADATA_CONTEXT_CACHE_KEY);
    }

    public static ExecutionContext getExecutionContext() {
        return new ExecutionContext(getCurrentOrganization(), getCurrentEnvironment());
    }

    public static ReferenceContext getCurrentContext() {
        if (getCurrentEnvironment() == null) {
            return new ReferenceContext(getCurrentOrganization(), ReferenceContextType.ORGANIZATION);
        }
        return new ReferenceContext(getCurrentEnvironment(), ReferenceContextType.ENVIRONMENT);
    }

    public static class ReferenceContext {

        String referenceId;
        ReferenceContextType referenceType;

        public ReferenceContext(String referenceId, ReferenceContextType referenceType) {
            this.referenceId = referenceId;
            this.referenceType = referenceType;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public ReferenceContextType getReferenceType() {
            return referenceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReferenceContext that = (ReferenceContext) o;
            return Objects.equals(referenceId, that.referenceId) && referenceType == that.referenceType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(referenceId, referenceType);
        }
    }

    public enum ReferenceContextType {
        ENVIRONMENT,
        ORGANIZATION,
    }
}
