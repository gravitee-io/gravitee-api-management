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
package io.gravitee.apim.core.invitation.model;

import io.gravitee.common.utils.TimeProvider;
import java.time.ZonedDateTime;
import java.util.Objects;
import lombok.Getter;

public final class ApplicationInvitation implements Invitation {

    private final InvitationId id;

    @Getter
    private final String applicationId;

    private final String email;

    @Getter
    private String roleName;

    @Getter
    private final ZonedDateTime createdAt;

    @Getter
    private ZonedDateTime updatedAt;

    private ApplicationInvitation(
        InvitationId id,
        String applicationId,
        String email,
        String roleName,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
    ) {
        this.id = id;
        this.applicationId = applicationId;
        this.email = email;
        this.roleName = roleName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ApplicationInvitation create(String applicationId, String email, String roleName) {
        var now = TimeProvider.now();
        return new ApplicationInvitation(InvitationId.random(), applicationId, email, roleName, now, now);
    }

    public static ApplicationInvitation of(
        InvitationId id,
        String applicationId,
        String email,
        String roleName,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
    ) {
        return new ApplicationInvitation(id, applicationId, email, roleName, createdAt, updatedAt);
    }

    public void updateRole(String roleName) {
        this.roleName = roleName;
        this.updatedAt = TimeProvider.now();
    }

    public void markResendAttempted() {
        this.updatedAt = TimeProvider.now();
    }

    @Override
    public InvitationId id() {
        return id;
    }

    public String applicationId() {
        return applicationId;
    }

    @Override
    public String email() {
        return email;
    }

    public String roleName() {
        return roleName;
    }

    public ZonedDateTime createdAt() {
        return createdAt;
    }

    public ZonedDateTime updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ApplicationInvitation that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "ApplicationInvitation{" +
            "id=" +
            id +
            ", applicationId='" +
            applicationId +
            '\'' +
            ", email='" +
            email +
            '\'' +
            ", roleName='" +
            roleName +
            '\'' +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            '}'
        );
    }
}
