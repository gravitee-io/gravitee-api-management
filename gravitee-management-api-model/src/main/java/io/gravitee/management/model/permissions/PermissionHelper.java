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
package io.gravitee.management.model.permissions;

import io.gravitee.management.model.MembershipType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PermissionHelper {

    private final static Map<MembershipType, List<ApiPermission>> apiPermissionsByRole = new HashMap<>();
    private final static Map<MembershipType, List<ApplicationPermission>> applicationPermissionsByRole = new HashMap<>();

    static {
        apiPermissionsByRole.put(MembershipType.USER, new ArrayList<ApiPermission>() {
            {
                add(ApiPermission.READ);
            }
        });

        applicationPermissionsByRole.put(MembershipType.USER, new ArrayList<ApplicationPermission>() {
            {
                add(ApplicationPermission.READ);
            }
        });

        apiPermissionsByRole.put(MembershipType.OWNER, new ArrayList<ApiPermission>() {
            {
                add(ApiPermission.READ);
                add(ApiPermission.MANAGE_API);
                add(ApiPermission.MANAGE_LIFECYCLE);
                add(ApiPermission.MANAGE_MEMBERS);
                add(ApiPermission.ANALYTICS);
                add(ApiPermission.MANAGE_API_KEYS);
                add(ApiPermission.MANAGE_PAGES);
            }
        });

        applicationPermissionsByRole.put(MembershipType.OWNER, new ArrayList<ApplicationPermission>() {
            {
                add(ApplicationPermission.READ);
                add(ApplicationPermission.MANAGE_API);
                add(ApplicationPermission.ANALYTICS);
                add(ApplicationPermission.MANAGE_API_KEYS);
                add(ApplicationPermission.MANAGE_MEMBERS);
            }
        });

        apiPermissionsByRole.put(MembershipType.PRIMARY_OWNER, new ArrayList<ApiPermission>() {
            {
                add(ApiPermission.READ);
                add(ApiPermission.MANAGE_API);
                add(ApiPermission.MANAGE_LIFECYCLE);
                add(ApiPermission.MANAGE_MEMBERS);
                add(ApiPermission.ANALYTICS);
                add(ApiPermission.MANAGE_API_KEYS);
                add(ApiPermission.MANAGE_PAGES);
                add(ApiPermission.DELETE);
                add(ApiPermission.TRANSFER_OWNERSHIP);
            }
        });

        applicationPermissionsByRole.put(MembershipType.PRIMARY_OWNER, new ArrayList<ApplicationPermission>() {
            {
                add(ApplicationPermission.READ);
                add(ApplicationPermission.MANAGE_API);
                add(ApplicationPermission.ANALYTICS);
                add(ApplicationPermission.MANAGE_API_KEYS);
                add(ApplicationPermission.MANAGE_MEMBERS);
                add(ApplicationPermission.DELETE);
            }
        });
    }

    public static List<ApiPermission> getApiPermissionsByRole(MembershipType type) {
        return apiPermissionsByRole.get(type);
    }

    public static List<ApplicationPermission> getApplicationPermissionsByRole(MembershipType type) {
        return applicationPermissionsByRole.get(type);
    }
}
