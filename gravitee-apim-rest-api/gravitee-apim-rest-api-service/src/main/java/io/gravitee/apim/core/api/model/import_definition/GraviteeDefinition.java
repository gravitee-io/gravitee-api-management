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
package io.gravitee.apim.core.api.model.import_definition;

import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.common.util.Version;
import io.gravitee.common.utils.TimeProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.Builder;

/**
 * Represents the definition of an exported API.
 */
public sealed interface GraviteeDefinition {
    Export export();
    ApiDescriptor api();
    Set<ApiMember> members();
    Set<NewApiMetadata> metadata();
    List<PageExport> pages();
    Collection<? extends PlanDescriptor> plans();
    List<Media> apiMedia();
    String apiPicture();
    String apiBackground();

    @Builder(toBuilder = true)
    record V4(
        Export export,
        ApiDescriptor.ApiDescriptorV4 api,
        Set<ApiMember> members,
        Set<NewApiMetadata> metadata,
        List<PageExport> pages,
        Collection<PlanDescriptor.PlanDescriptorV4> plans,
        List<Media> apiMedia,
        String apiPicture,
        String apiBackground
    )
        implements GraviteeDefinition {
        public V4(
            ApiDescriptor.ApiDescriptorV4 api,
            Set<ApiMember> members,
            Set<NewApiMetadata> metadata,
            List<PageExport> pages,
            Collection<PlanDescriptor.PlanDescriptorV4> plans,
            List<Media> apiMedia,
            String apiPicture,
            String apiBackground
        ) {
            this(new Export(), api, members, metadata, pages, plans, apiMedia, apiPicture, apiBackground);
        }
    }

    @Builder(toBuilder = true)
    record Native(
        Export export,
        ApiDescriptor.ApiDescriptorNative api,
        Set<ApiMember> members,
        Set<NewApiMetadata> metadata,
        List<PageExport> pages,
        Collection<PlanDescriptor.PlanDescriptorV4> plans,
        List<Media> apiMedia,
        String apiPicture,
        String apiBackground
    )
        implements GraviteeDefinition {
        public Native(
            ApiDescriptor.ApiDescriptorNative api,
            Set<ApiMember> members,
            Set<NewApiMetadata> metadata,
            List<PageExport> pages,
            Collection<PlanDescriptor.PlanDescriptorV4> plans,
            List<Media> apiMedia,
            String apiPicture,
            String apiBackground
        ) {
            this(new Export(), api, members, metadata, pages, plans, apiMedia, apiPicture, apiBackground);
        }
    }

    @Builder(toBuilder = true)
    record GraviteeDefinitionFederated(
        Export export,
        ApiDescriptor.ApiDescriptorFederated api,
        Set<ApiMember> members,
        Set<NewApiMetadata> metadata,
        List<PageExport> pages,
        Collection<PlanDescriptor.PlanDescriptorV4> plans,
        List<Media> apiMedia,
        String apiPicture,
        String apiBackground
    )
        implements GraviteeDefinition {
        public GraviteeDefinitionFederated(
            ApiDescriptor.ApiDescriptorFederated api,
            Set<ApiMember> members,
            Set<NewApiMetadata> metadata,
            List<PageExport> pages,
            Collection<PlanDescriptor.PlanDescriptorV4> plans,
            List<Media> apiMedia,
            String apiPicture,
            String apiBackground
        ) {
            this(new Export(), api, members, metadata, pages, plans, apiMedia, apiPicture, apiBackground);
        }
    }

    record Export(Instant date, String apimVersion) {
        public Export() {
            this(TimeProvider.instantNow(), Version.RUNTIME_VERSION.MAJOR_VERSION);
        }
    }
}
