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
package io.gravitee.rest.api.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PrimaryOwnerEntity {
    @ApiModelProperty(
            value = "The user id.",
            example = "005197cc-cc84-86a6-a75a-88f9772c67db")
    private final String id;

    @ApiModelProperty(
            value = "The user email.",
            example = "contact@gravitee.io")
    private final String email;

    @ApiModelProperty(
            value = "The user display name.",
            example = "John Doe")
    private final String displayName;

    public PrimaryOwnerEntity(UserEntity user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }
}
