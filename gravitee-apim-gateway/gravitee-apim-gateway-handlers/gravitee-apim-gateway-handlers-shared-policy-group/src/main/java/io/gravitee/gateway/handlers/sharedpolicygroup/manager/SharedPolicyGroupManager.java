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
package io.gravitee.gateway.handlers.sharedpolicygroup.manager;

import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import java.util.Collection;

public interface SharedPolicyGroupManager {
    /**
     * Register an API definition. It is a "create or update" operation, if the api was previously existing, the
     * definition is updated accordingly.
     * @param sharedPolicyGroup
     * @return
     */
    boolean register(ReactableSharedPolicyGroup sharedPolicyGroup);

    void unregister(String sharedPolicyGroupId);

    void refresh();

    /**
     * Returns a collection of deployed {@link ReactableSharedPolicyGroup}s.
     * @return A collection of deployed  {@link ReactableSharedPolicyGroup}s.
     */
    Collection<ReactableSharedPolicyGroup> sharedPolicyGroups();

    /**
     * Retrieve a deployed {@link ReactableSharedPolicyGroup} using its ID.
     * @param sharedPolicyGroupId The ID of the deployed Shared Policy Group.
     * @return A deployed {@link ReactableSharedPolicyGroup}
     */
    ReactableSharedPolicyGroup get(String sharedPolicyGroupId);
}
