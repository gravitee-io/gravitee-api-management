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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.media.model.Media;
import io.gravitee.rest.api.model.MediaEntity;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MediaAdapter {
    MediaAdapter INSTANCE = Mappers.getMapper(MediaAdapter.class);

    Media toCoreModel(MediaEntity mediaEntity);
    List<Media> toCoreModels(List<MediaEntity> mediaEntities);

    MediaEntity toEntity(Media media);
    List<MediaEntity> toEntities(List<Media> mediaList);

    List<Media> mapMedia(Collection<io.gravitee.repository.media.model.Media> src);

    @Mapping(target = "apiId", source = "api")
    Media mediaToMedia(io.gravitee.repository.media.model.Media media);
}
