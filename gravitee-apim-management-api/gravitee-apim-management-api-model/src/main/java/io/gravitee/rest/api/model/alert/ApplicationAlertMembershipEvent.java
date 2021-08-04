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
package io.gravitee.rest.api.model.alert;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationAlertMembershipEvent {

    private final Set<String> applicationIds;
    private final Set<String> groupIds;

    public ApplicationAlertMembershipEvent(Set<String> applicationIds, Set<String> groupIds) {
        this.applicationIds = applicationIds != null ? applicationIds : new HashSet<>();
        this.groupIds = groupIds != null ? groupIds : new HashSet<>();
    }

    public Set<String> getApplicationIds() {
        return applicationIds;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }
}
