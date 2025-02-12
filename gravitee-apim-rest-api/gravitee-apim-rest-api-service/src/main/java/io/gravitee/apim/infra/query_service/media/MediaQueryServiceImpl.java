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
package io.gravitee.apim.infra.query_service.media;

import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.media.query_service.MediaQueryService;
import io.gravitee.apim.infra.adapter.MediaAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MediaQueryServiceImpl extends AbstractService implements MediaQueryService {

    private final MediaRepository mediaRepository;

    public MediaQueryServiceImpl(@Lazy MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    public List<Media> findAllByApiId(String apiId) {
        try {
            return MediaAdapter.INSTANCE.mapMedia(mediaRepository.findAllByApi(apiId));
        } catch (TechnicalException e) {
            log.error("An error as occurred trying to find medias for API " + apiId, e);
            throw new TechnicalManagementException("An error as occurred trying to find medias", e);
        }
    }
}
