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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Guillaume GILLON
 * @author GraviteeSource Team
 */
public interface PageService {
    Page<PageEntity> findAll(Pageable pageable);

    PageEntity findById(String pageId);

    PageEntity findById(String pageId, String acceptedLocale);

    default List<PageEntity> findByApi(String environmentId, String apiId) {
        return search(environmentId, new PageQuery.Builder().api(apiId).build(), false);
    }

    List<PageEntity> search(String environmentId, PageQuery query);

    List<PageEntity> search(String environmentId, PageQuery query, boolean withTranslations);

    List<PageEntity> search(String environmentId, PageQuery query, String acceptedLocale);

    void transformSwagger(ExecutionContext executionContext, PageEntity pageEntity);

    void transformSwagger(ExecutionContext executionContext, PageEntity pageEntity, GenericApiEntity apiEntity);

    PageEntity createPage(ExecutionContext executionContext, String apiId, NewPageEntity page);

    PageEntity createPage(ExecutionContext executionContext, String apiId, NewPageEntity newPageEntity, String newPageId);

    PageEntity createPage(ExecutionContext executionContext, NewPageEntity page);

    PageEntity update(ExecutionContext executionContext, String pageId, UpdatePageEntity updatePageEntity);

    PageEntity update(ExecutionContext executionContext, String pageId, UpdatePageEntity updatePageEntity, boolean partial);

    void delete(ExecutionContext executionContext, String pageId);

    void deleteAllByApi(ExecutionContext executionContext, String apiId);

    int findMaxApiPageOrderByApi(String apiId);

    int findMaxPortalPageOrder(String referenceId);

    void fetchAll(ExecutionContext executionContext, PageQuery query, String contributor);

    long execAutoFetch(ExecutionContext executionContext);

    PageEntity fetch(ExecutionContext executionContext, String pageId, String contributor);

    List<PageEntity> importFiles(ExecutionContext executionContext, ImportPageEntity pageEntity);

    List<PageEntity> importFiles(ExecutionContext executionContext, String apiId, ImportPageEntity pageEntity);

    void transformWithTemplate(ExecutionContext executionContext, PageEntity pageEntity, String api);

    PageEntity create(ExecutionContext executionContext, String apiId, PageEntity pageEntity);

    List<String> validateSafeContent(ExecutionContext executionContext, PageEntity pageEntity, String apiId);

    Map<SystemFolderType, String> initialize(ExecutionContext executionContext);

    PageEntity createAsideFolder(ExecutionContext executionContext, String apiId);

    PageEntity createSystemFolder(ExecutionContext executionContext, String apiId, SystemFolderType systemFolderType, int order);

    PageEntity createWithDefinition(ExecutionContext executionContext, String apiId, String toString);

    void createOrUpdatePages(ExecutionContext executionContext, List<PageEntity> pages, String apiId);

    void createOrUpdateSwaggerPage(
        ExecutionContext executionContext,
        String apiId,
        ImportSwaggerDescriptorEntity swaggerDescriptor,
        boolean isForCreation
    );

    /**
     * Check if the page is used as GeneralCondition by an active Plan for the given ApiID
     *
     *
     * @param executionContext
     * @param page
     * @param apiId
     * @return
     */
    boolean isPageUsedAsGeneralConditions(ExecutionContext executionContext, PageEntity page, String apiId);

    boolean shouldHaveRevision(String pageType);

    Optional<PageEntity> attachMedia(String pageId, String mediaId, String mediaName);

    boolean folderHasPublishedChildren(String folderId);

    boolean isMediaUsedInPages(ExecutionContext executionContext, String mediaHash);
}
