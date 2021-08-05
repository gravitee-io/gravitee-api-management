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
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume GILLON
 * @author GraviteeSource Team
 */
@Component
public class MediaRepositoryProxy extends AbstractProxy<MediaRepository> implements MediaRepository {

    @Override
    public Media create(Media media) throws TechnicalException {
        return target.create(media);
    }

    @Override
    public void deleteAllByApi(String api) {
        target.deleteAllByApi(api);
    }

    @Override
    public Optional<Media> findByHash(String hash) {
        return target.findByHash(hash);
    }

    @Override
    public Optional<Media> findByHash(String hash, boolean withContent) {
        return target.findByHash(hash, withContent);
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api) {
        return target.findByHashAndApi(hash, api);
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api, boolean withContent) {
        return target.findByHashAndApi(hash, api, withContent);
    }

    @Override
    public Optional<Media> findByHashAndType(String hash, String mediaType) {
        return target.findByHashAndType(hash, mediaType);
    }

    @Override
    public Optional<Media> findByHashAndType(String hash, String mediaType, boolean withContent) {
        return target.findByHashAndType(hash, mediaType, withContent);
    }

    @Override
    public Optional<Media> findByHashAndApiAndType(String hash, String api, String mediaType) {
        return target.findByHashAndApiAndType(hash, api, mediaType);
    }

    @Override
    public Optional<Media> findByHashAndApiAndType(String hash, String api, String mediaType, boolean withContent) {
        return target.findByHashAndApiAndType(hash, api, mediaType, withContent);
    }

    @Override
    public List<Media> findAllByApi(String api) {
        return target.findAllByApi(api);
    }
}
