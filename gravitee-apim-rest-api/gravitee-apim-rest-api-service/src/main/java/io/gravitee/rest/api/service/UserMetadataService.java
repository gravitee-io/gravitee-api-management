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

import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.rest.api.model.NewUserMetadataEntity;
import io.gravitee.rest.api.model.UpdateUserMetadataEntity;
import io.gravitee.rest.api.model.UserMetadataEntity;
import java.util.List;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface UserMetadataService {
    UserMetadataEntity create(NewUserMetadataEntity metadata);
    UserMetadataEntity update(UpdateUserMetadataEntity metadata);
    List<UserMetadataEntity> findAllByUserId(String userId);
    void deleteAllByCustomFieldId(String key, String refId, CustomUserFieldReferenceType refType);
}
