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

import io.gravitee.rest.api.model.MediaEntity;

import java.util.List;

/**
 * @author Guillaume Gillon
 */
public interface MediaService {

    String savePortalMedia(MediaEntity imageEntity);

    String saveApiMedia(String api, MediaEntity imageEntity);

    MediaEntity findByHash(String hash);

    MediaEntity findByHashAndApiId(String hash, String apiId);

    Long getMediaMaxSize();

    List<MediaEntity> findAllByApiId(String apiId);

    String createWithDefinition(String api, String definition);

    void deleteAllByApi(String apiId);

}
