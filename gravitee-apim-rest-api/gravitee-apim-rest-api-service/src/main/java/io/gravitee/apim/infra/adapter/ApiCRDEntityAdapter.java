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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.api.ApiCRDEntity;
import io.gravitee.rest.api.model.api.ApiCRDEntity.Member;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(builder = @Builder(disableBuilder = true))
public interface ApiCRDEntityAdapter {
    ApiCRDEntityAdapter INSTANCE = Mappers.getMapper(ApiCRDEntityAdapter.class);

    MemberCRD map(Member member);
    Member map(MemberCRD crd);

    @Mapping(target = "configuration", expression = "java(convertToJsonNode(source))")
    PageSourceEntity map(PageCRD.PageSource source);

    @Mapping(target = "configurationMap", expression = "java(deserializeSourceConfiguration(source))")
    PageCRD.PageSource map(PageSourceEntity source);

    PageCRD map(ApiCRDEntity.PageCRD crd);
    ApiCRDEntity.PageCRD map(PageCRD crd);

    Set<MemberCRD> toMemberCRDs(List<Member> member);
    List<Member> toApiCRDMembers(Set<MemberCRD> crd);
    Map<String, PageCRD> toCoreApiCRDPages(Map<String, ApiCRDEntity.PageCRD> crd);
    Map<String, ApiCRDEntity.PageCRD> toRestApiCRDPages(Map<String, PageCRD> crd);

    default JsonNode convertToJsonNode(PageCRD.PageSource source) {
        if (source == null || source.getConfiguration() == null) {
            return null;
        }
        try {
            return GraviteeJacksonMapper.getInstance().readTree(source.getConfiguration());
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected error while converting the configuration to JSON Node", ioe);
        }
    }

    default Map<String, Object> deserializeSourceConfiguration(PageSourceEntity source) {
        if (source.getConfiguration() == null) {
            return Map.of();
        }

        try {
            return GraviteeJacksonMapper.getInstance().readValue(source.getConfiguration(), Map.class);
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected error while converting the configuration to Map object", ioe);
        }
    }
}
