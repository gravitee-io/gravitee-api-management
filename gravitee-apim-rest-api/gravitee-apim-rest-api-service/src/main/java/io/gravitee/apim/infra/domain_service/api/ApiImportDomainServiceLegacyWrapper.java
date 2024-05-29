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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.domain_service.ApiImportDomainService;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.infra.adapter.MediaAdapter;
import io.gravitee.apim.infra.adapter.MemberAdapter;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class ApiImportDomainServiceLegacyWrapper implements ApiImportDomainService {

    private final ApiImportExportService apiImportExportService;

    @Override
    public void createPageAndMedia(List<Media> mediaList, String apiId) {
        apiImportExportService.createPageAndMedia(
            GraviteeContext.getExecutionContext(),
            apiId,
            MediaAdapter.INSTANCE.toEntities(mediaList)
        );
    }

    @Override
    public void createMembers(Set<ApiMember> members, String apiId) {
        apiImportExportService.createMembers(GraviteeContext.getExecutionContext(), apiId, MemberAdapter.INSTANCE.toEntities(members));
    }
}
