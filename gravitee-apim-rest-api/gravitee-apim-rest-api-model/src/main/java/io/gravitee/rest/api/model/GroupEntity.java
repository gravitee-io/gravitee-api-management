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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupEntity {

    @EqualsAndHashCode.Include
    private String id;

    private String name;

    @JsonProperty("event_rules")
    private List<GroupEventRuleEntity> eventRules;

    boolean manageable;
    private Map<RoleScope, String> roles;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("max_invitation")
    private Integer maxInvitation;

    @JsonProperty("lock_api_role")
    private boolean lockApiRole;

    @JsonProperty("lock_application_role")
    private boolean lockApplicationRole;

    @JsonProperty("system_invitation")
    private boolean systemInvitation;

    @JsonProperty("email_invitation")
    private boolean emailInvitation;

    @JsonProperty("disable_membership_notifications")
    private boolean disableMembershipNotifications;

    private String apiPrimaryOwner;

    @JsonProperty("primary_owner")
    private boolean primaryOwner;

    private String origin = OriginContext.Origin.MANAGEMENT.name();
}
