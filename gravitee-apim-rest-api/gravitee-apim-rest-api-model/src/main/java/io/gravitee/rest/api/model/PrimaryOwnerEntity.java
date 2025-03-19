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

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrimaryOwnerEntity implements Serializable {

    @Schema(description = "The user or group id.", example = "005197cc-cc84-86a6-a75a-88f9772c67db")
    private String id;

    @Schema(description = "The user or group email.", example = "contact@gravitee.io")
    private String email;

    @Schema(description = "The user or group display name.", example = "John Doe")
    private String displayName;

    @Schema(description = "The primary owner type", example = "USER")
    private String type;

    public PrimaryOwnerEntity(UserEntity user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.type = "USER";
    }

    public PrimaryOwnerEntity(GroupEntity group, String mail) {
        this.id = group.getId();
        this.email = mail;
        this.displayName = group.getName();
        this.type = "GROUP";
    }
}
