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
package io.gravitee.apim.core.portal.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalAddressDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.domain_service.PortalLogoDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNameDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RenamePortalUseCaseTest {

    private static final String ENVIRONMENT = "environment";
    private final PortalCrudServiceInMemory portalCrudService = spy(new PortalCrudServiceInMemory());
    private final RenamePortalUseCase useCase = new RenamePortalUseCase(
        portalCrudService,
        new PortalNameDomainService(portalCrudService),
        new PortalSummaryDomainService(
            new PortalLogoDomainService(new ThemeQueryServiceInMemory()),
            new PortalNavigationItemsQueryServiceInMemory(),
            new PortalAddressDomainServiceInMemory(portalCrudService, new InstallationAccessQueryServiceInMemory())
        )
    );

    @Test
    void should_return_not_found_if_portal_disappears_before_the_write() {
        var portal = existingPortal();
        var newName = "Partner Portal";
        doAnswer(invocation -> {
            portalCrudService.delete(portal.getId());
            return invocation.callRealMethod();
        })
            .when(portalCrudService)
            .updateName(portal.getId(), ENVIRONMENT, newName);

        assertThatThrownBy(() -> useCase.execute(new RenamePortalUseCase.Input(ENVIRONMENT, portal.getId(), newName))).isInstanceOf(
            PortalNotFoundException.class
        );
        assertThat(portalCrudService.findByIdAndEnvironmentId(portal.getId(), ENVIRONMENT)).isEmpty();
    }

    @Test
    void should_reject_a_name_that_cannot_fit_in_the_jdbc_column() {
        var portal = existingPortal();

        assertThatThrownBy(() -> useCase.execute(new RenamePortalUseCase.Input(ENVIRONMENT, portal.getId(), "a".repeat(257)))).isInstanceOf(
            ValidationDomainException.class
        );
        assertThat(portalCrudService.findByIdAndEnvironmentId(portal.getId(), ENVIRONMENT)).hasValueSatisfying(stored ->
            assertThat(stored.getName()).isEqualTo("Partners")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "\u00A0", "\u202F", "\u200B", " \u00A0\u200B\uFEFF " })
    void should_reject_names_consisting_only_of_whitespace_and_format_characters(String name) {
        var portal = existingPortal();

        assertThatThrownBy(() -> useCase.execute(new RenamePortalUseCase.Input(ENVIRONMENT, portal.getId(), name))).isInstanceOf(
            ValidationDomainException.class
        );
        verify(portalCrudService, never()).updateName(any(), anyString(), anyString());
    }

    @Test
    void should_accept_a_name_at_the_256_utf16_code_unit_limit() {
        var portal = existingPortal();
        var name = "\uD801\uDC00".repeat(128);

        var result = useCase.execute(new RenamePortalUseCase.Input(ENVIRONMENT, portal.getId(), name));

        assertThat(result.portal().getName()).isEqualTo(name);
        assertThat(portalCrudService.findByIdAndEnvironmentId(portal.getId(), ENVIRONMENT)).hasValueSatisfying(stored ->
            assertThat(stored.getName()).isEqualTo(name)
        );
    }

    @Test
    void should_preserve_format_characters_in_names_with_text() {
        var portal = existingPortal();
        var name = "Partner\u200DPortal";

        var result = useCase.execute(new RenamePortalUseCase.Input(ENVIRONMENT, portal.getId(), name));

        assertThat(result.portal().getName()).isEqualTo(name);
    }

    @ParameterizedTest
    @CsvSource({ "Partners,Partners", "' Partners ',' Partners '", "Partners,' Partners '", "Partners,'\u00A0Partners\u200B'" })
    void should_keep_the_same_name_without_writing_even_if_another_portal_has_that_name(String storedName, String requestedName) {
        var portal = portal(storedName);
        portalCrudService.initWith(List.of(portal, portal(storedName)));

        var result = useCase.execute(new RenamePortalUseCase.Input(ENVIRONMENT, portal.getId(), requestedName));

        assertThat(result.portal().getName()).isEqualTo(storedName);
        verify(portalCrudService, never()).updateName(any(), anyString(), anyString());
    }

    private Portal existingPortal() {
        var portal = portal("Partners");
        portalCrudService.initWith(List.of(portal));
        return portal;
    }

    private static Portal portal(String name) {
        return Portal.create(ENVIRONMENT, "organization", name, "partners-access-point");
    }
}
