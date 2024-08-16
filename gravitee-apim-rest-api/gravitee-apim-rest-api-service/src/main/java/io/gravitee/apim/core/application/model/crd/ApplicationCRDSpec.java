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
package io.gravitee.apim.core.application.model.crd;

import io.gravitee.definition.model.Origin;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ApplicationCRDSpec extends ApplicationEntity {

    private List<ApplicationMetadataCRD> metadata;
    private List<ApplicationCRDMember> members;

    public NewApplicationEntity toNewApplicationEntity() {
        NewApplicationEntity nae = new NewApplicationEntity();
        nae.setId(getId());
        nae.setName(getName());
        nae.setDescription(getDescription());
        nae.setType(getType());
        nae.setDomain(getDomain());
        nae.setApiKeyMode(getApiKeyMode());
        nae.setBackground(getBackground());
        nae.setPicture(getPicture());
        nae.setOrigin(Origin.KUBERNETES);
        nae.setGroups(getGroups());
        nae.setSettings(getSettings());

        return nae;
    }

    public UpdateApplicationEntity toUpdateApplicationEntity() {
        UpdateApplicationEntity uae = new UpdateApplicationEntity();

        uae.setName(getName());
        uae.setDescription(getDescription());
        uae.setType(getType());
        uae.setDomain(getDomain());
        uae.setApiKeyMode(getApiKeyMode());
        uae.setBackground(getBackground());
        uae.setPicture(getPicture());
        uae.setGroups(getGroups());
        uae.setSettings(getSettings());
        uae.setDisableMembershipNotifications(isDisableMembershipNotifications());

        return uae;
    }
}
