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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.rest.api.management.v2.rest.model.ChannelSelector;
import io.gravitee.rest.api.management.v2.rest.model.ConditionSelector;
import io.gravitee.rest.api.management.v2.rest.model.FlowV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpSelector;
import io.gravitee.rest.api.management.v2.rest.model.Media;
import io.gravitee.rest.api.management.v2.rest.model.Page;
import io.gravitee.rest.api.management.v2.rest.model.PageMedia;
import io.gravitee.rest.api.management.v2.rest.model.Revision;
import io.gravitee.rest.api.management.v2.rest.model.Selector;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface PageMapper {
    PageMapper INSTANCE = Mappers.getMapper(PageMapper.class);

    @Mapping(target = "name", source = "mediaName")
    @Mapping(target = "hash", source = "mediaHash")
    PageMedia convertMedia(PageMediaEntity pageMediaEntity);

    @Mapping(target = "id", source = "pageId")
    Revision convertRevision(PageEntity.PageRevisionId pageRevisionId);

    @Mapping(target = "contentRevision", source = "contentRevisionId")
    @Mapping(target = "updatedAt", source = "lastModificationDate")
    Page convert(PageEntity pageEntity);

    Set<Page> convertListToSet(List<PageEntity> pageEntityList);

    @Mapping(target = "createdAt", source = "createAt")
    Media convertMediaEntityToMedia(MediaEntity mediaEntity);

    List<Media> convertMediaList(List<MediaEntity> mediaEntity);
}
