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
package io.gravitee.apim.core.notification.model;

import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class PrimaryOwnerNotificationTemplateData {

    private String id;
    private String displayName;
    private String email;
    private String type;

    public static PrimaryOwnerNotificationTemplateData from(PrimaryOwnerEntity primaryOwner) {
        return PrimaryOwnerNotificationTemplateData.builder()
            .id(primaryOwner.id())
            .displayName(primaryOwner.displayName())
            .email(primaryOwner.email())
            .type(primaryOwner.type().name())
            .build();
    }
}
