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

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewPreRegisterUserEntity extends NewExternalUserEntity {

    @Schema(
        description = "Whether this user is a service account. A service account must not have a firstname, and " +
            "requires at least one of sourceId, lastname or email to derive a unique identifier."
    )
    private boolean service = false;

    public boolean isService() {
        return service;
    }

    public void setService(boolean service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return (
            "NewPreRegisterUserEntity{" +
            "firstname='" +
            getFirstname() +
            '\'' +
            ", lastname='" +
            getLastname() +
            '\'' +
            ", email='" +
            getEmail() +
            '\'' +
            ", source='" +
            getSource() +
            '\'' +
            ", picture='" +
            getPicture() +
            '\'' +
            ", sourceId='" +
            getSourceId() +
            '\'' +
            ", newsletter=" +
            getNewsletter() +
            ", customFields=" +
            getCustomFields() +
            ", isService=" +
            service +
            '}'
        );
    }
}
