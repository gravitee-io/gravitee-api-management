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
package io.gravitee.repository.media.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.media.model.Media;

import java.util.List;
import java.util.Optional;

/**
 * @author Guillaume Gillon
 * @author GraviteeSource Team
 */
public interface MediaRepository {

    Optional<Media> findByHash(String hash, String mediaType);

    Optional<Media> findByHashAndApi(String hash, String api, String mediaType);

    List<Media> findAllByApi(String api);

    Media create(Media media) throws TechnicalException;

    void deleteAllByApi(String api);
}
