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
package io.gravitee.apim.core.portal_page.query_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal_page.model.ExpandsViewContext;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageFactory;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageQueryServiceDefaultMethodTest {

    private static class RecordingService implements PortalPageQueryService {

        String recordedEnv;
        PortalViewContext recordedCtx;
        List<ExpandsViewContext> recordedExpands;
        List<PortalPageWithViewDetails> toReturn = new ArrayList<>();

        @Override
        public List<PortalPageWithViewDetails> findByEnvironmentIdAndContext(
            String environmentId,
            PortalViewContext context,
            List<ExpandsViewContext> expand
        ) {
            this.recordedEnv = environmentId;
            this.recordedCtx = context;
            this.recordedExpands = expand;
            return toReturn;
        }

        @Override
        public PortalPageWithViewDetails findById(PageId pageId) {
            return null;
        }
    }

    @Test
    void default_overload_should_delegate_with_null_expands() {
        // Given
        var service = new RecordingService();
        var pageId = PageId.random();
        PortalPage page = PortalPageFactory.create(pageId, new GraviteeMarkdown("content"));
        service.toReturn.add(new PortalPageWithViewDetails(page, new PortalPageView(PortalViewContext.HOMEPAGE, true)));

        // When
        var result = service.findByEnvironmentIdAndContext("DEFAULT", PortalViewContext.HOMEPAGE);

        // Then
        assertThat(service.recordedEnv).isEqualTo("DEFAULT");
        assertThat(service.recordedCtx).isEqualTo(PortalViewContext.HOMEPAGE);
        assertThat(service.recordedExpands).isNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().viewDetails().context()).isEqualTo(PortalViewContext.HOMEPAGE);
    }
}
