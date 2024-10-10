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
package io.gravitee.rest.api.service.converter;

import io.gravitee.rest.api.model.NewPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.UpdatePageEntity;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class PageConverter {

    public UpdatePageEntity toUpdatePageEntity(PageEntity pageEntity) {
        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setCrossId(pageEntity.getCrossId());
        updatePageEntity.setConfiguration(pageEntity.getConfiguration());
        updatePageEntity.setContent(pageEntity.getContent());
        updatePageEntity.setExcludedAccessControls(pageEntity.isExcludedAccessControls());
        updatePageEntity.setAccessControls(pageEntity.getAccessControls());
        updatePageEntity.setHomepage(pageEntity.isHomepage());
        updatePageEntity.setLastContributor(pageEntity.getLastContributor());
        updatePageEntity.setName(pageEntity.getName());
        updatePageEntity.setOrder(pageEntity.getOrder());
        updatePageEntity.setParentId(pageEntity.getParentId());
        updatePageEntity.setPublished(pageEntity.isPublished());
        updatePageEntity.setVisibility(pageEntity.getVisibility());
        updatePageEntity.setSource(pageEntity.getSource());
        updatePageEntity.setAttachedMedia(pageEntity.getAttachedMedia());
        updatePageEntity.setMetadata(pageEntity.getMetadata());
        return updatePageEntity;
    }

    public NewPageEntity toNewPageEntity(PageEntity pageEntity) {
        return toNewPageEntity(pageEntity, false);
    }

    public NewPageEntity toNewPageEntity(PageEntity pageEntity, boolean resetCrossId) {
        NewPageEntity newPage = new NewPageEntity();
        newPage.setCrossId(resetCrossId ? null : pageEntity.getCrossId());
        newPage.setConfiguration(pageEntity.getConfiguration());
        newPage.setContent(pageEntity.getContent());
        newPage.setExcludedAccessControls(pageEntity.isExcludedAccessControls());
        newPage.setAccessControls(pageEntity.getAccessControls());
        newPage.setHomepage(pageEntity.isHomepage());
        newPage.setLastContributor(pageEntity.getLastContributor());
        newPage.setName(pageEntity.getName());
        newPage.setOrder(pageEntity.getOrder());
        newPage.setParentId(pageEntity.getParentId());
        newPage.setPublished(pageEntity.isPublished());
        newPage.setSource(pageEntity.getSource());
        newPage.setType(PageType.valueOf(pageEntity.getType()));
        newPage.setAttachedMedia(pageEntity.getAttachedMedia());
        newPage.setVisibility(pageEntity.getVisibility());
        return newPage;
    }
}
