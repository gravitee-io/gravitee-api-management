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
package io.gravitee.apim.infra.crud_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.model.PortalPageContext;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageViewCrudServiceImplTest {

    private PortalPageContextRepository contextRepository;
    private PortalPageContextCrudService service;

    @BeforeEach
    void setUp() {
        contextRepository = mock(PortalPageContextRepository.class);
        service = new PortalPageContextCrudServiceImpl(contextRepository);
    }

    @Test
    @SneakyThrows
    void should_return_empty_list_when_no_contexts_found() {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of());
        assertThat(service.findAllIdsByContextTypeAndEnvironmentId(PortalViewContext.HOMEPAGE, "env1")).isEmpty();
    }

    @Test
    @SneakyThrows
    void should_return_contexts_for_type_and_env() {
        var pid1 = PageId.random();
        var pid2 = PageId.random();
        PortalPageContext ctx1 = new PortalPageContext();
        ctx1.setId("ctx1");
        ctx1.setPageId(pid1.toString());
        PortalPageContext ctx2 = new PortalPageContext();
        ctx2.setId("ctx2");
        ctx2.setPageId(pid2.toString());
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of(ctx1, ctx2));
        assertThat(service.findAllIdsByContextTypeAndEnvironmentId(PortalViewContext.HOMEPAGE, "env1"))
            .containsExactlyInAnyOrder(pid1, pid2);
    }

    @Test
    @SneakyThrows
    void should_handle_exception_and_throw_domain_exception() {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenThrow(new TechnicalException("fail"));
        assertThatThrownBy(() -> service.findAllIdsByContextTypeAndEnvironmentId(PortalViewContext.HOMEPAGE, "env1"))
            .isInstanceOf(io.gravitee.apim.core.exception.TechnicalDomainException.class)
            .hasMessage("Something went wrong while trying to find portal page contexts");
    }
}
