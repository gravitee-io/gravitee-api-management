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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "groups")
public class GroupMongo extends Auditable {

    @Id
    private String id;

    private String environmentId;
    private String name;
    private List<String> administrators;
    private List<GroupEventRuleMongo> eventRules;
    private Integer maxInvitation;
    private boolean lockApiRole;
    private boolean lockApplicationRole;
    private boolean systemInvitation;
    private boolean emailInvitation;
    private boolean disableMembershipNotifications;
    private String apiPrimaryOwner;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GroupEventRuleMongo> getEventRules() {
        return eventRules;
    }

    public void setEventRules(List<GroupEventRuleMongo> eventRules) {
        this.eventRules = eventRules;
    }

    public List<String> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(List<String> administrators) {
        this.administrators = administrators;
    }

    public Integer getMaxInvitation() {
        return maxInvitation;
    }

    public void setMaxInvitation(Integer maxInvitation) {
        this.maxInvitation = maxInvitation;
    }

    public boolean isLockApiRole() {
        return lockApiRole;
    }

    public void setLockApiRole(boolean lockApiRole) {
        this.lockApiRole = lockApiRole;
    }

    public boolean isLockApplicationRole() {
        return lockApplicationRole;
    }

    public void setLockApplicationRole(boolean lockApplicationRole) {
        this.lockApplicationRole = lockApplicationRole;
    }

    public boolean isSystemInvitation() {
        return systemInvitation;
    }

    public void setSystemInvitation(boolean systemInvitation) {
        this.systemInvitation = systemInvitation;
    }

    public boolean isEmailInvitation() {
        return emailInvitation;
    }

    public void setEmailInvitation(boolean emailInvitation) {
        this.emailInvitation = emailInvitation;
    }

    public boolean isDisableMembershipNotifications() {
        return disableMembershipNotifications;
    }

    public void setDisableMembershipNotifications(boolean disableMembershipNotifications) {
        this.disableMembershipNotifications = disableMembershipNotifications;
    }

    public String getApiPrimaryOwner() {
        return apiPrimaryOwner;
    }

    public void setApiPrimaryOwner(String apiPrimaryOwner) {
        this.apiPrimaryOwner = apiPrimaryOwner;
    }
}
