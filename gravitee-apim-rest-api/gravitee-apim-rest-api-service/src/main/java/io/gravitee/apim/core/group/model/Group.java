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
package io.gravitee.apim.core.group.model;

import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data
@Builder(toBuilder = true)
@With
public class Group {

    private String id;
    private String environmentId;
    private String name;
    private List<GroupEventRule> eventRules;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private Integer maxInvitation;
    private boolean lockApiRole;
    private boolean lockApplicationRole;
    private boolean systemInvitation;
    private boolean emailInvitation;
    private boolean disableMembershipNotifications;

    /** User id that will be the PrimaryOwner of API */
    private String apiPrimaryOwner;

    public record GroupEventRule(GroupEvent event) {}

    public enum GroupEvent {
        API_CREATE,
        APPLICATION_CREATE,
    }
}
