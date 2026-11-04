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
package io.gravitee.apim.core.portal_notification.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A notification written to the portal inbox of a user. {@code environmentId} is null for an organization-level
 * trigger and for notifications created before 4.14, which also have no hook, API or application.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PortalNotification {

    private String id;
    private String title;
    private String message;
    private String user;
    private Date createdAt;
    private Date readAt;
    private String hook;
    private String apiId;
    private String applicationId;
    private String environmentId;

    public boolean isRead() {
        return readAt != null;
    }
}
