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
package io.gravitee.apim.core.documentation.model.factory;

import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageModelFactory {

    private PageModelFactory() {}

    public static Page fromCRDSpec(PageCRD pageCRD) {
        return Page
            .builder()
            .id(pageCRD.getId())
            .name(pageCRD.getName())
            .crossId(pageCRD.getCrossId())
            .parentId(pageCRD.getParentId())
            .type(Page.Type.valueOf(pageCRD.getType().name()))
            .visibility(Page.Visibility.valueOf(pageCRD.getVisibility().name()))
            .order(pageCRD.getOrder())
            .published(pageCRD.isPublished())
            .content(pageCRD.getContent())
            .homepage(pageCRD.isHomepage())
            .configuration(pageCRD.getConfiguration())
            .source(
                pageCRD.getSource() == null
                    ? null
                    : new PageSource(
                        pageCRD.getSource().getType(),
                        pageCRD.getSource().getConfiguration(),
                        pageCRD.getSource().getConfigurationMap()
                    )
            )
            .build();
    }

    public static PageCRD toCRDSpec(Page page) {
        return PageCRD
            .builder()
            .id(page.getId())
            .name(page.getName())
            .crossId(page.getCrossId())
            .parentId(page.getParentId())
            .type(PageCRD.Type.valueOf(page.getType().name()))
            .visibility(PageCRD.Visibility.valueOf(page.getVisibility().name()))
            .order(page.getOrder())
            .published(page.isPublished())
            .content(page.getContent())
            .homepage(page.isHomepage())
            .configuration(page.getConfiguration())
            .source(
                page.getSource() == null
                    ? null
                    : new PageCRD.PageSource(
                        page.getSource().getType(),
                        page.getSource().getConfiguration(),
                        page.getSource().getConfigurationMap()
                    )
            )
            .build();
    }
}
