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

import io.gravitee.rest.api.model.NewTagEntity;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.model.UpdateTagEntity;
import java.util.List;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface TagService {
    List<TagEntity> findByReference(String referenceId, TagReferenceType referenceType);
    TagEntity findByIdAndReference(String tagId, String referenceId, TagReferenceType referenceType);
    TagEntity create(NewTagEntity tag, String referenceId, TagReferenceType referenceType);
    TagEntity update(UpdateTagEntity tag, String referenceId, TagReferenceType referenceType);
    List<TagEntity> create(List<NewTagEntity> tags, String referenceId, TagReferenceType referenceType);
    List<TagEntity> update(List<UpdateTagEntity> tags, String referenceId, TagReferenceType referenceType);
    void delete(String tagId, String referenceId, TagReferenceType referenceType);
    Set<String> findByUser(String user, String referenceId, TagReferenceType referenceType);
}
