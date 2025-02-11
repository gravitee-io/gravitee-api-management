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
        implements GraviteeDefinition {}

    @Builder(toBuilder = true)
    record Native(
        Export export,
        ApiDescriptor.Native api,
        Set<ApiMember> members,
        Set<NewApiMetadata> metadata,
        List<PageExport> pages,
        Collection<PlanDescriptor.PlanDescriptorV4> plans,
        List<Media> apiMedia,
        String apiPicture,
        String apiBackground
    )
        implements GraviteeDefinition {}

    @Builder(toBuilder = true)
    record Federated(
        Export export,
        ApiDescriptor.Federated api,
        Set<ApiMember> members,
        Set<NewApiMetadata> metadata,
        List<PageExport> pages,
        Collection<PlanDescriptor.PlanDescriptorV4> plans,
        List<Media> apiMedia,
        String apiPicture,
        String apiBackground
    )
        implements GraviteeDefinition {}

    @Builder(toBuilder = true)
    record V2(
        Export export,
        ApiDescriptor.ApiDescriptorV2 api,
        Set<ApiMember> members,
        Set<NewApiMetadata> metadata,
        List<PageExport> pages,
        Collection<PlanDescriptor.PlanDescriptorV4> plans,
        List<Media> apiMedia,
        String apiPicture,
        String apiBackground
    )
        implements GraviteeDefinition {}

    record Export(Instant date, String apimVersion) {
        public Export() {
            this(TimeProvider.instantNow(), Version.RUNTIME_VERSION.MAJOR_VERSION);
        }
    }

    static GraviteeDefinition from(
        ApiDescriptor api,
        Set<ApiMember> members,
        Set<NewApiMetadata> metadata,
        List<PageExport> pages,
        Collection<PlanDescriptor.PlanDescriptorV4> plans,
        List<Media> media,
        String picture,
        String bckgrnd
    ) {
        var export = new Export();
        return switch (api) {
            case ApiDescriptor.ApiDescriptorV2 v2 -> new V2(export, v2, members, metadata, pages, plans, media, picture, bckgrnd);
            case ApiDescriptor.ApiDescriptorV4 v4 -> new V4(export, v4, members, metadata, pages, plans, media, picture, bckgrnd);
            case ApiDescriptor.Federated fed -> new Federated(export, fed, members, metadata, pages, plans, media, picture, bckgrnd);
            case ApiDescriptor.Native nativeApi -> new Native(export, nativeApi, members, metadata, pages, plans, media, picture, bckgrnd);
        };
    }
}
