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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.InvitationEntity;
import io.gravitee.rest.api.model.InvitationReferenceType;
import io.gravitee.rest.api.model.NewInvitationEntity;
import io.gravitee.rest.api.model.UpdateInvitationEntity;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface InvitationService {
    InvitationEntity create(NewInvitationEntity invitation);
    InvitationEntity update(UpdateInvitationEntity invitation);
    List<InvitationEntity> findByReference(InvitationReferenceType referenceType, String referenceId);
    void delete(String invitationId, String referenceId);
    List<InvitationEntity> findAll();
    void addMember(String referenceType, String referenceId, String userId, String apiRole, String applicationRole);
}
