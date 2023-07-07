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
package io.gravitee.rest.api.management.v2.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.management.v2.rest.model.Media;
import io.gravitee.rest.api.management.v2.rest.model.Page;
import io.gravitee.rest.api.management.v2.rest.model.PageMedia;
import io.gravitee.rest.api.management.v2.rest.model.PageSource;
import io.gravitee.rest.api.management.v2.rest.model.Revision;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class })
public interface PageMapper {
    PageMapper INSTANCE = Mappers.getMapper(PageMapper.class);

    @Mapping(target = "name", source = "mediaName")
    @Mapping(target = "hash", source = "mediaHash")
    PageMedia mapPageMedia(PageMediaEntity pageMediaEntity);

    @Mapping(target = "mediaName", source = "name")
    @Mapping(target = "mediaHash", source = "hash")
    PageMediaEntity mapPageMedia(PageMedia pageMedia);

    @Mapping(target = "id", source = "pageId")
    Revision mapPageRevision(PageEntity.PageRevisionId pageRevisionId);

    @Mapping(target = "pageId", source = "id")
    PageEntity.PageRevisionId mapPageRevision(Revision revision);

    @Mapping(target = "contentRevision", source = "contentRevisionId")
    @Mapping(target = "updatedAt", source = "lastModificationDate")
    Page map(PageEntity pageEntity);

    @Mapping(target = "contentRevisionId", source = "contentRevision")
    @Mapping(target = "lastModificationDate", source = "updatedAt")
    PageEntity map(Page page);

    @Mapping(target = "configuration", qualifiedByName = "deserializeJsonConfiguration")
    PageSourceEntity mapPageSource(PageSource source);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    PageSource mapPageSource(PageSourceEntity sourceEntity);

    Set<Page> mapToSet(List<PageEntity> pageEntityList);

    @Mapping(target = "createdAt", source = "createAt")
    Media mapMedia(MediaEntity mediaEntity);

    @Mapping(target = "uploadDate", source = "createdAt")
    MediaEntity mapMedia(Media media);

    @Named("deserializeJsonConfiguration")
    default JsonNode deserializeJsonConfiguration(Object configuration) throws JsonProcessingException {
        if (Objects.isNull(configuration)) {
            return null;
        }
        if (configuration instanceof LinkedHashMap) {
            ObjectMapper mapper = new GraviteeMapper();
            try {
                return mapper.valueToTree(configuration);
            } catch (IllegalArgumentException e) {
                throw new TechnicalManagementException("An error occurred while trying to parse connector configuration " + e);
            }
        }
        return null;
    }
}
