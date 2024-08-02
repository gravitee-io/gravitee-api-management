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

import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

/**
 * @author Guillaume Gillon
 * @author GraviteeSource Team
 */
public interface MediaService {
    String savePortalMedia(ExecutionContext executionContext, MediaEntity imageEntity);

    String saveApiMedia(ExecutionContext context, String api, MediaEntity imageEntity);

    MediaEntity findByHash(ExecutionContext context, String hash);
    MediaEntity findByHashAndApiId(String hash, String apiId);
    MediaEntity findByHash(ExecutionContext context, String id, boolean ignoreType);
    MediaEntity findByHashAndApi(String id, String api, boolean ignoreType);

    List<MediaEntity> findAllWithoutContent(ExecutionContext context, List<PageMediaEntity> pageMediaEntities);
    List<MediaEntity> findAllWithoutContent(List<PageMediaEntity> pageMediaEntities, String api);

    Long getMediaMaxSize(ExecutionContext executionContext);

    List<MediaEntity> findAllByApiId(String apiId);

    String createWithDefinition(ExecutionContext executionContext, String api, String definition);

    void deleteAllByApi(String apiId);

    void deleteByHashAndApi(String hash, String apiId);
}
