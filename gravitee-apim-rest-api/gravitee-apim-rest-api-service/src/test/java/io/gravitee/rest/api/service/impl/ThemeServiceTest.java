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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.THEME;
import static io.gravitee.repository.management.model.Theme.AuditEvent.THEME_CREATED;
import static io.gravitee.repository.management.model.Theme.AuditEvent.THEME_DELETED;
import static io.gravitee.repository.management.model.Theme.AuditEvent.THEME_RESET;
import static io.gravitee.repository.management.model.Theme.AuditEvent.THEME_UPDATED;
import static io.gravitee.repository.management.model.ThemeReferenceType.ENVIRONMENT;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.infra.domain_service.theme.ThemeImageWriteValidatorImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.management.model.ThemeType;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.theme.GenericThemeEntity;
import io.gravitee.rest.api.model.theme.portal.NewThemeEntity;
import io.gravitee.rest.api.model.theme.portal.ThemeCssDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeCssType;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.model.theme.portal.UpdateThemeEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DuplicateThemeNameException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.ThemeNotFoundException;
import io.gravitee.rest.api.service.impl.ThemeServiceImpl.ThemeDefinitionMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ThemeServiceTest {

    private static final String THEME_ID = "my-theme-id";
    private static final String THEMES_PATH = "src/test/resources/themes";

    @InjectMocks
    private ThemeService themeService = new ThemeServiceImpl();

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private AuditService auditService;

    private ThemeServiceImpl themeServiceImpl = new ThemeServiceImpl();

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(themeService, "themesPath", THEMES_PATH);
        ReflectionTestUtils.setField(themeService, "themeImageWriteValidator", new ThemeImageWriteValidatorImpl());
        ReflectionTestUtils.setField(themeServiceImpl, "themesPath", THEMES_PATH);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldFindById() throws TechnicalException, JsonProcessingException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(List.of());
        String definition = definitionMapper.writeValueAsString(themeDefinition);

        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.getType()).thenReturn(ThemeType.PORTAL);
        when(theme.getDefinition()).thenReturn(definition);
        when(theme.getReferenceId()).thenReturn("DEFAULT");
        when(theme.getCreatedAt()).thenReturn(new Date(1));
        when(theme.getUpdatedAt()).thenReturn(new Date(2));
        when(theme.getFavicon()).thenReturn("favicon.png");
        when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));

        final ThemeEntity themeEntity = (ThemeEntity) themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID);
        assertEquals(THEME_ID, themeEntity.getId());
        assertEquals("NAME", themeEntity.getName());
        assertEquals(definition, definitionMapper.writeValueAsString(themeEntity.getDefinition()));
        assertEquals(new Date(1), themeEntity.getCreatedAt());
        assertEquals(new Date(2), themeEntity.getUpdatedAt());
        assertEquals(themeEntity.getFavicon(), theme.getFavicon());
    }

    @Test
    public void shouldThrowThemeNotFoundException() throws TechnicalException {
        assertThrows(ThemeNotFoundException.class, () -> {
            when(themeRepository.findById(THEME_ID)).thenReturn(empty());
            themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID);
        });
    }

    @Test
    public void shouldThrowThemeNotFoundExceptionWhenThemeIsNotInDefaultEnv() throws TechnicalException, JsonProcessingException {
        assertThrows(ThemeNotFoundException.class, () -> {
            ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
            ThemeDefinition themeDefinition = new ThemeDefinition();
            themeDefinition.setData(List.of());
            String definition = definitionMapper.writeValueAsString(themeDefinition);

            final Theme theme = mock(Theme.class);
            when(theme.getReferenceId()).thenReturn("NOT-DEFAULT");
            when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));

            final ThemeEntity themeEntity = (ThemeEntity) themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID);
            assertEquals(THEME_ID, themeEntity.getId());
            assertEquals("NAME", themeEntity.getName());
            assertEquals(definition, definitionMapper.writeValueAsString(themeEntity.getDefinition()));
            assertEquals(new Date(1), themeEntity.getCreatedAt());
            assertEquals(new Date(2), themeEntity.getUpdatedAt());
        });
    }

    @Test
    public void shouldFindAll() throws TechnicalException, JsonProcessingException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        String definition = themeServiceImpl.getDefaultDefinition();
        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.getType()).thenReturn(ThemeType.PORTAL);
        when(theme.getDefinition()).thenReturn(definition);
        when(theme.getCreatedAt()).thenReturn(new Date(1));
        when(theme.getUpdatedAt()).thenReturn(new Date(2));
        when(
            themeRepository.findByReferenceIdAndReferenceTypeAndType(
                GraviteeContext.getCurrentEnvironment(),
                ENVIRONMENT.name(),
                ThemeType.PORTAL
            )
        ).thenReturn(singleton(theme));

        final Set<GenericThemeEntity> themes = themeService.findAllByType(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.theme.ThemeType.PORTAL
        );
        final ThemeEntity themeEntity = (ThemeEntity) themes.iterator().next();
        assertEquals(THEME_ID, themeEntity.getId());
        assertEquals("NAME", themeEntity.getName());
        assertTrue(definitionMapper.isSame(definition, definitionMapper.writeValueAsString(themeEntity.getDefinition())));
        assertEquals(new Date(1), themeEntity.getCreatedAt());
        assertEquals(new Date(2), themeEntity.getUpdatedAt());
    }

    @Test
    public void shouldFindEnabled() throws TechnicalException {
        String definition = themeServiceImpl.getDefaultDefinition();
        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.isEnabled()).thenReturn(true);
        when(theme.getDefinition()).thenReturn(definition);
        when(theme.getCreatedAt()).thenReturn(new Date(1));
        when(theme.getUpdatedAt()).thenReturn(new Date(2));
        when(
            themeRepository.findByReferenceIdAndReferenceTypeAndType(
                GraviteeContext.getCurrentEnvironment(),
                ENVIRONMENT.name(),
                ThemeType.PORTAL
            )
        ).thenReturn(singleton(theme));

        assertNotNull(themeService.findEnabledPortalTheme(GraviteeContext.getExecutionContext()));
    }

    @Test
    public void shouldFindEnabledThrowTechnicalManagementExceptionOnRepositoryException() throws TechnicalException {
        assertThrows(TechnicalManagementException.class, () -> {
            when(
                themeRepository.findByReferenceIdAndReferenceTypeAndType(
                    GraviteeContext.getCurrentEnvironment(),
                    ENVIRONMENT.name(),
                    ThemeType.PORTAL
                )
            ).thenThrow(TechnicalException.class);

            themeService.findEnabledPortalTheme(GraviteeContext.getExecutionContext());
        });
    }

    @Test
    public void shouldFindEnabledOverNotEnabled() throws TechnicalException {
        Set<Theme> databaseThemes = new HashSet<>();

        Theme theme1 = new Theme();
        theme1.setId("theme-1");
        theme1.setEnabled(false);
        theme1.setDefinition("{}");
        theme1.setType(ThemeType.PORTAL);
        databaseThemes.add(theme1);

        Theme theme2 = new Theme();
        theme2.setId("theme-2");
        theme2.setType(ThemeType.PORTAL);
        theme2.setEnabled(true);
        theme2.setDefinition("{}");
        databaseThemes.add(theme2);

        when(
            themeRepository.findByReferenceIdAndReferenceTypeAndType(
                GraviteeContext.getCurrentEnvironment(),
                ENVIRONMENT.name(),
                ThemeType.PORTAL
            )
        ).thenReturn(databaseThemes);

        ThemeEntity resultTheme = themeService.findOrCreateDefaultPortalTheme(GraviteeContext.getExecutionContext());
        assertNotNull(resultTheme);
        assertEquals("theme-2", resultTheme.getId());

        verify(themeRepository, never()).create(any());
    }

    @Test
    public void shouldFindNotEnabled() throws TechnicalException {
        Set<Theme> databaseThemes = new HashSet<>();

        Theme theme1 = new Theme();
        theme1.setId("theme-1");
        theme1.setType(ThemeType.PORTAL);
        theme1.setEnabled(false);
        theme1.setDefinition("{}");
        databaseThemes.add(theme1);

        when(
            themeRepository.findByReferenceIdAndReferenceTypeAndType(
                GraviteeContext.getCurrentEnvironment(),
                ENVIRONMENT.name(),
                ThemeType.PORTAL
            )
        ).thenReturn(databaseThemes);

        ThemeEntity resultTheme = themeService.findOrCreateDefaultPortalTheme(GraviteeContext.getExecutionContext());
        assertNotNull(resultTheme);
        assertEquals("theme-1", resultTheme.getId());

        verify(themeRepository, never()).create(any());
    }

    @Test
    public void shouldFindAndCreateDefaultTheme() throws TechnicalException {
        when(
            themeRepository.findByReferenceIdAndReferenceTypeAndType(
                GraviteeContext.getCurrentEnvironment(),
                ENVIRONMENT.name(),
                ThemeType.PORTAL
            )
        ).thenReturn(new HashSet<>());
        when(themeRepository.create(any())).thenAnswer(i -> i.getArgument(0));

        ThemeEntity resultTheme = themeService.findOrCreateDefaultPortalTheme(GraviteeContext.getExecutionContext());
        assertNotNull(resultTheme);
        assertEquals("Default theme", resultTheme.getName());

        verify(themeRepository).create(argThat(theme -> "Default theme".equals(theme.getName())));
    }

    @Test
    public void shouldFindOrCreateDefaultThrowTechnicalManagementExceptionOnRepositoryException() throws TechnicalException {
        assertThrows(TechnicalManagementException.class, () -> {
            when(
                themeRepository.findByReferenceIdAndReferenceTypeAndType(
                    GraviteeContext.getCurrentEnvironment(),
                    ENVIRONMENT.name(),
                    ThemeType.PORTAL
                )
            ).thenThrow(TechnicalException.class);

            themeService.findOrCreateDefaultPortalTheme(GraviteeContext.getExecutionContext());
        });
    }

    @Test
    public void shouldGetDefaultIfNoThemeEnabled() throws TechnicalException {
        when(
            themeRepository.findByReferenceIdAndReferenceTypeAndType(
                GraviteeContext.getCurrentEnvironment(),
                ENVIRONMENT.name(),
                ThemeType.PORTAL
            )
        ).thenReturn(new HashSet<>());

        assertNotNull(themeService.findEnabledPortalTheme(GraviteeContext.getExecutionContext()));
    }

    @Test
    public void shouldCreate() throws TechnicalException, IOException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(List.of());
        String definition = definitionMapper.writeValueAsString(themeDefinition);
        final NewThemeEntity newThemeEntity = new NewThemeEntity();
        newThemeEntity.setName("NAME");
        newThemeEntity.setDefinition(themeDefinition);

        final Theme createdTheme = new Theme();
        createdTheme.setId(THEME_ID);
        createdTheme.setName("NAME");
        createdTheme.setType(ThemeType.PORTAL);
        createdTheme.setDefinition(definition);
        createdTheme.setCreatedAt(new Date());
        createdTheme.setUpdatedAt(new Date());
        when(themeRepository.create(any())).thenReturn(createdTheme);

        final ThemeEntity themeEntity = themeService.createPortalTheme(GraviteeContext.getExecutionContext(), newThemeEntity);

        assertNotNull(themeEntity.getId());
        assertEquals("NAME", themeEntity.getName());
        assertNotNull(themeEntity.getDefinition());
        assertEquals(0, themeEntity.getDefinition().getData().size());
        assertNotNull(themeEntity.getCreatedAt());
        assertNotNull(themeEntity.getUpdatedAt());

        final Theme theme = new Theme();
        theme.setName("NAME");
        theme.setType(ThemeType.PORTAL);
        theme.setDefinition(definition);
        theme.setReferenceId("REF_ID");
        theme.setReferenceType(ENVIRONMENT.name());

        verify(themeRepository, times(1)).create(
            argThat(argument ->
                ("NAME".equals(argument.getName()) &&
                    argument.getDefinition() != null &&
                    "DEFAULT".equals(argument.getReferenceId()) &&
                    ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                    !argument.getId().isEmpty() &&
                    argument.getCreatedAt() != null &&
                    argument.getUpdatedAt() != null)
            )
        );
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(ImmutableMap.of(THEME, THEME_ID)) &&
                    auditLogData.getEvent().equals(THEME_CREATED) &&
                    auditLogData.getOldValue() == null
            )
        );
    }

    @Test
    public void shouldThrowDuplicateThemeNameExceptionOnCreate() throws TechnicalException {
        assertThrows(DuplicateThemeNameException.class, () -> {
            final Theme theme = mock(Theme.class);
            when(theme.getId()).thenReturn(THEME_ID);
            when(theme.getName()).thenReturn("NAME");
            when(theme.getType()).thenReturn(ThemeType.PORTAL);
            when(theme.getDefinition()).thenReturn(themeServiceImpl.getDefaultDefinition());
            when(
                themeRepository.findByReferenceIdAndReferenceTypeAndType(
                    GraviteeContext.getCurrentEnvironment(),
                    ENVIRONMENT.name(),
                    ThemeType.PORTAL
                )
            ).thenReturn(singleton(theme));

            final NewThemeEntity newThemeEntity = new NewThemeEntity();
            newThemeEntity.setName("NAME");
            themeService.createPortalTheme(GraviteeContext.getExecutionContext(), newThemeEntity);
        });
    }

    @Test
    public void shouldUpdate() throws TechnicalException, JsonProcessingException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(List.of());
        String definition = definitionMapper.writeValueAsString(themeDefinition);

        final UpdateThemeEntity updateThemeEntity = new UpdateThemeEntity();
        updateThemeEntity.setId(THEME_ID);
        updateThemeEntity.setName("NAME");
        updateThemeEntity.setDefinition(themeDefinition);

        final Theme updatedTheme = new Theme();
        updatedTheme.setId(THEME_ID);
        updatedTheme.setName("NAME");
        updatedTheme.setType(ThemeType.PORTAL);
        updatedTheme.setDefinition(definition);
        updatedTheme.setCreatedAt(new Date());
        updatedTheme.setUpdatedAt(new Date());
        updatedTheme.setReferenceType(ENVIRONMENT.name());
        updatedTheme.setReferenceId(GraviteeContext.getCurrentEnvironment());
        when(themeRepository.update(any())).thenReturn(updatedTheme);
        when(themeRepository.findById(THEME_ID)).thenReturn(of(updatedTheme));

        final ThemeEntity themeEntity = themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), updateThemeEntity);

        assertNotNull(themeEntity.getId());
        assertEquals("NAME", themeEntity.getName());
        assertEquals(definition, definitionMapper.writeValueAsString(themeEntity.getDefinition()));
        assertNotNull(themeEntity.getCreatedAt());
        assertNotNull(themeEntity.getUpdatedAt());

        final Theme theme = new Theme();
        theme.setName("NAME");
        theme.setDefinition(definition);
        theme.setReferenceId("REF_ID");
        theme.setReferenceType(ENVIRONMENT.name());

        verify(themeRepository, times(1)).update(
            argThat(
                argument ->
                    "NAME".equals(argument.getName()) &&
                    argument.getDefinition() != null &&
                    "DEFAULT".equals(argument.getReferenceId()) &&
                    ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                    THEME_ID.equals(argument.getId()) &&
                    argument.getUpdatedAt() != null
            )
        );

        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(ImmutableMap.of(THEME, THEME_ID)) && auditLogData.getEvent().equals(THEME_UPDATED)
            )
        );
    }

    @Test
    public void shouldNotUpdateBecauseDoesNotBelongToEnvironment() throws TechnicalException, JsonProcessingException {
        assertThrows(ThemeNotFoundException.class, () -> {
            ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
            ThemeDefinition themeDefinition = new ThemeDefinition();
            themeDefinition.setData(List.of());
            String definition = definitionMapper.writeValueAsString(themeDefinition);

            final UpdateThemeEntity updateThemeEntity = new UpdateThemeEntity();
            updateThemeEntity.setId(THEME_ID);
            updateThemeEntity.setName("NAME");
            updateThemeEntity.setDefinition(themeDefinition);

            final Theme updatedTheme = new Theme();
            updatedTheme.setId(THEME_ID);
            updatedTheme.setName("NAME");
            updatedTheme.setDefinition(definition);
            updatedTheme.setCreatedAt(new Date());
            updatedTheme.setUpdatedAt(new Date());
            updatedTheme.setReferenceType(ENVIRONMENT.name());
            updatedTheme.setReferenceId("Another_environment");
            when(themeRepository.findById(THEME_ID)).thenReturn(of(updatedTheme));

            themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), updateThemeEntity);

            verify(themeRepository, never()).update(any());

            verify(auditService, never()).createAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                argThat(
                    auditLogData ->
                        auditLogData.getProperties().equals(ImmutableMap.of(THEME, THEME_ID)) &&
                        auditLogData.getEvent().equals(THEME_UPDATED)
                )
            );
        });
    }

    @Test
    public void shouldThrowDuplicateThemeNameExceptionOnUpdate() throws TechnicalException {
        assertThrows(DuplicateThemeNameException.class, () -> {
            final Theme theme = mock(Theme.class);
            when(theme.getId()).thenReturn(THEME_ID);
            when(theme.getName()).thenReturn("NAME");
            when(theme.getType()).thenReturn(ThemeType.PORTAL);
            when(theme.getDefinition()).thenReturn(themeServiceImpl.getDefaultDefinition());
            when(theme.getReferenceType()).thenReturn(ENVIRONMENT.name());
            when(theme.getReferenceId()).thenReturn(GraviteeContext.getCurrentEnvironment());

            final Theme theme2 = mock(Theme.class);
            when(theme2.getId()).thenReturn("foobar");
            when(theme2.getName()).thenReturn("NAME");
            when(theme2.getType()).thenReturn(ThemeType.PORTAL);
            when(theme2.getDefinition()).thenReturn(themeServiceImpl.getDefaultDefinition());

            when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));
            when(
                themeRepository.findByReferenceIdAndReferenceTypeAndType(
                    GraviteeContext.getCurrentEnvironment(),
                    ENVIRONMENT.name(),
                    ThemeType.PORTAL
                )
            ).thenReturn(new HashSet(asList(theme, theme2)));

            final UpdateThemeEntity updateThemeEntity = new UpdateThemeEntity();
            updateThemeEntity.setId(THEME_ID);
            updateThemeEntity.setName("NAME");
            themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), updateThemeEntity);
        });
    }

    @Test
    public void shouldNotUpdate() throws TechnicalException {
        final UpdateThemeEntity updateThemeEntity = new UpdateThemeEntity();
        updateThemeEntity.setId(THEME_ID);
        when(themeRepository.findById(THEME_ID)).thenReturn(empty());

        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.getDefinition()).thenReturn(themeServiceImpl.getDefaultDefinition());
        when(themeRepository.create(any())).thenReturn(theme);

        themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), updateThemeEntity);
        verify(themeRepository).create(any());
    }

    @Test
    public void shouldResetToDefaultTheme() throws TechnicalException {
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(List.of());

        final Theme theme = new Theme();
        theme.setId(THEME_ID);
        theme.setName("NAME");
        theme.setType(ThemeType.PORTAL);
        theme.setDefinition(themeServiceImpl.getDefinition(THEMES_PATH + "/custom-definition.json"));
        theme.setReferenceId("DEFAULT");
        theme.setReferenceType(ENVIRONMENT.name());
        theme.setCreatedAt(new Date());
        theme.setUpdatedAt(new Date());

        when(themeRepository.findById(THEME_ID)).thenReturn(Optional.of(theme));
        when(themeRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        themeService.resetToDefaultTheme(GraviteeContext.getExecutionContext(), THEME_ID);

        verify(themeRepository, times(1)).delete(THEME_ID);

        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(ImmutableMap.of(THEME, THEME_ID)) && auditLogData.getEvent().equals(THEME_RESET)
            )
        );
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        when(theme.getReferenceType()).thenReturn(ENVIRONMENT.name());
        when(theme.getReferenceId()).thenReturn(GraviteeContext.getCurrentEnvironment());
        when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));

        themeService.delete(GraviteeContext.getExecutionContext(), THEME_ID);

        verify(themeRepository, times(1)).delete(THEME_ID);
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(ImmutableMap.of(THEME, THEME_ID)) &&
                    auditLogData.getEvent().equals(THEME_DELETED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue() == theme
            )
        );
    }

    @Test
    public void shouldNotDeleteBecauseDoesNotBelongToEnvironment() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        when(theme.getReferenceType()).thenReturn(ENVIRONMENT.name());
        when(theme.getReferenceId()).thenReturn("Another_environment");
        when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));

        themeService.delete(GraviteeContext.getExecutionContext(), THEME_ID);

        verify(themeRepository, never()).delete(THEME_ID);
        verify(auditService, never()).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(ImmutableMap.of(THEME, THEME_ID)) &&
                    auditLogData.getEvent().equals(THEME_DELETED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(theme)
            )
        );
    }

    @Test
    public void shouldLoadDefaultThemeDefinition() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String definition = themeServiceImpl.getDefaultDefinition();
        ThemeDefinition themeDefinition = mapper.readValue(definition, ThemeDefinition.class);
        assertNotNull(themeDefinition);
        assertNotNull(themeDefinition.getData());
        assertEquals(38, themeDefinition.getData().size());
    }

    @Test
    public void shouldMergeThemeDefinition() throws IOException {
        ThemeDefinitionMapper mapper = new ThemeDefinitionMapper();
        String def = themeServiceImpl.getDefinition(THEMES_PATH + "/base-definition.json");
        ThemeDefinition baseDefinition = mapper.readValue(def, ThemeDefinition.class);
        String customDef = themeServiceImpl.getDefinition(THEMES_PATH + "/custom-definition.json");
        ThemeDefinition customDefinition = mapper.readValue(customDef, ThemeDefinition.class);
        assertEquals(33, customDefinition.getData().size());
        assertNull(mapper.getThemeComponentDefinition(baseDefinition, "gv-pagination"));
        assertNotNull(mapper.getThemeComponentDefinition(customDefinition, "gv-pagination"));
        assertEquals(mapper.getThemeComponentDefinition(baseDefinition, "gv-plans").getCss().size(), 5);
        assertEquals(mapper.getThemeComponentDefinition(customDefinition, "gv-plans").getCss().size(), 4);
        assertEquals(mapper.getThemeComponentDefinition(baseDefinition, "gv-popover").getCss().size(), 2);
        assertEquals(mapper.getThemeComponentDefinition(customDefinition, "gv-popover").getCss().size(), 3);
        ThemeCssDefinition gvThemeColor = mapper.getThemeCssDefinition(baseDefinition, "gv-theme", "--gv-theme-color");
        assertNull(gvThemeColor.getDefaultValue());
        assertEquals(gvThemeColor.getType(), ThemeCssType.COLOR);
        assertEquals(gvThemeColor.getValue(), "#009B5B");
        ThemeCssDefinition gvButtonFz = mapper.getThemeCssDefinition(baseDefinition, "gv-button", "--gv-button--fz");
        assertNull(gvButtonFz.getDefaultValue());
        assertEquals(gvButtonFz.getType(), ThemeCssType.LENGTH);
        assertEquals(gvButtonFz.getValue(), "var(--gv-theme-font-size-m, 14px)");
        assertEquals(gvButtonFz.getDescription(), "Font size");

        ThemeDefinition mergedDefinition = mapper.merge(def, customDef);

        assertEquals(34, mergedDefinition.getData().size());
        assertNull(mapper.getThemeComponentDefinition(mergedDefinition, "gv-pagination"));
        assertEquals(mapper.getThemeComponentDefinition(mergedDefinition, "gv-plans").getCss().size(), 5);
        assertEquals(mapper.getThemeComponentDefinition(mergedDefinition, "gv-popover").getCss().size(), 2);
        ThemeCssDefinition gvThemeColorMerged = mapper.getThemeCssDefinition(mergedDefinition, "gv-theme", "--gv-theme-color");
        assertNull(gvThemeColorMerged.getDefaultValue());
        assertEquals(gvThemeColorMerged.getType(), ThemeCssType.COLOR);
        assertEquals(gvThemeColorMerged.getValue(), "#FAFAFA");
        ThemeCssDefinition gvButtonFzMerged = mapper.getThemeCssDefinition(mergedDefinition, "gv-button", "--gv-button--fz");
        assertNull(gvButtonFzMerged.getDefaultValue());
        assertEquals(gvButtonFzMerged.getType(), ThemeCssType.LENGTH);
        assertEquals(gvButtonFzMerged.getValue(), "200px");
        assertEquals(gvButtonFzMerged.getDescription(), "Font size");
    }

    @Test
    public void shouldMergeThemeDefinitionWithLegacy() throws IOException, TechnicalException {
        ThemeDefinitionMapper mapper = new ThemeDefinitionMapper();
        String def = themeServiceImpl.getDefaultDefinition();
        ThemeDefinition themeDefinition = mapper.readValue(def, ThemeDefinition.class);
        String customDef = themeServiceImpl.getDefinition(THEMES_PATH + "/legacy-definition.json");
        ThemeDefinition legacyDefinition = mapper.readValue(customDef, ThemeDefinition.class);
        assertEquals(38, themeDefinition.getData().size());
        assertEquals(35, legacyDefinition.getData().size());

        ThemeDefinition mergedDefinition = mapper.merge(def, customDef);
        assertNotNull(mergedDefinition);
        assertEquals(38, mergedDefinition.getData().size());

        assertNotNull(mapper.getThemeCssDefinition(legacyDefinition, "gv-theme", "--gv-theme--c"));
        assertNull(mapper.getThemeCssDefinition(themeDefinition, "gv-theme", "--gv-theme--c"));
        assertNull(mapper.getThemeCssDefinition(mergedDefinition, "gv-theme", "--gv-theme--c"));

        assertNull(mapper.getThemeCssDefinition(legacyDefinition, "gv-theme", "--gv-theme-color"));
        assertNotNull(mapper.getThemeCssDefinition(themeDefinition, "gv-theme", "--gv-theme-color"));
        assertNotNull(mapper.getThemeCssDefinition(mergedDefinition, "gv-theme", "--gv-theme-color"));
    }

    @Test
    public void shouldCompareDefinition() throws IOException, TechnicalException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        String definition = themeServiceImpl.getDefaultDefinition();

        ThemeDefinition themeDefinition = definitionMapper.readPortalDefinition(definition);
        String formattedDefinition = definitionMapper.writerWithDefaultPrettyPrinter().writeValueAsString(themeDefinition);

        assertNotEquals(definition, formattedDefinition);
        assertTrue(definitionMapper.isSame(definition, formattedDefinition));
        assertFalse(definitionMapper.isSame(definition, themeServiceImpl.getDefinition(THEMES_PATH + "/custom-definition.json")));
    }

    @Test
    public void shouldCreateDefaultTheme() throws TechnicalException, IOException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        String definition = themeServiceImpl.getDefaultDefinition();
        final UpdateThemeEntity themeToCreate = new UpdateThemeEntity();
        themeToCreate.setId(THEME_ID);
        themeToCreate.setName("Default");
        themeToCreate.setDefinition(definitionMapper.readPortalDefinition(definition));

        final Theme createdTheme = new Theme();
        createdTheme.setId(THEME_ID);
        createdTheme.setName("Default");
        createdTheme.setType(ThemeType.PORTAL);
        createdTheme.setDefinition(definition);
        createdTheme.setCreatedAt(new Date());
        createdTheme.setUpdatedAt(new Date());
        when(themeRepository.create(any())).thenReturn(createdTheme);
        assertEquals(definitionMapper.readTree(definition), definitionMapper.readTree(definition));
        assertEquals(definition, definition);

        themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), themeToCreate);

        verify(themeRepository, times(1)).create(
            argThat(argument -> {
                try {
                    return (
                        "Default".equals(argument.getName()) &&
                        definitionMapper.readTree(argument.getDefinition()).equals(definitionMapper.readTree(definition)) &&
                        "DEFAULT".equals(argument.getReferenceId()) &&
                        ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                        !argument.getId().isEmpty() &&
                        argument.getCreatedAt() != null &&
                        argument.getUpdatedAt() != null
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            })
        );
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(ImmutableMap.of(THEME, THEME_ID)) &&
                    auditLogData.getEvent().equals(THEME_CREATED) &&
                    auditLogData.getOldValue() == null
            )
        );
    }

    @Test
    public void shouldUpdateDefaultTheme() throws TechnicalException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ThemeDefinitionMapper themeDefinitionMapper = new ThemeDefinitionMapper();
        String definition = themeServiceImpl.getDefaultDefinition();

        final Theme theme = mock(Theme.class);
        when(theme.getDefinition()).thenReturn(definition);

        final Theme theme2 = mock(Theme.class);
        when(theme2.getId()).thenReturn(THEME_ID);
        when(theme2.getName()).thenReturn("NAME");
        String customDefinition = themeServiceImpl.getDefinition(THEMES_PATH + "/custom-definition.json");
        when(theme2.getDefinition()).thenReturn(customDefinition);
        when(theme2.getReferenceType()).thenReturn(ENVIRONMENT.name());
        when(theme2.getReferenceId()).thenReturn("DEFAULT");
        when(theme2.getCreatedAt()).thenReturn(new Date(1));
        when(theme2.getUpdatedAt()).thenReturn(new Date(2));
        when(theme2.getType()).thenReturn(ThemeType.PORTAL);

        when(
            themeRepository.findByReferenceIdAndReferenceTypeAndType(
                GraviteeContext.getCurrentEnvironment(),
                ENVIRONMENT.name(),
                ThemeType.PORTAL
            )
        ).thenReturn(new HashSet(asList(theme, theme2)));

        String mergeDefinition = themeDefinitionMapper.writeValueAsString(themeDefinitionMapper.merge(definition, customDefinition));

        themeService.updateDefaultPortalTheme(GraviteeContext.getExecutionContext());

        verify(themeRepository, times(1)).update(
            argThat(argument -> {
                try {
                    return (
                        "NAME".equals(argument.getName()) &&
                        mapper.readTree(argument.getDefinition()).equals(mapper.readTree(mergeDefinition)) &&
                        "DEFAULT".equals(argument.getReferenceId()) &&
                        ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                        !argument.getId().isEmpty() &&
                        argument.getCreatedAt() != null &&
                        argument.getUpdatedAt() != null
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            })
        );
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(ImmutableMap.of(THEME, THEME_ID)) && auditLogData.getEvent().equals(THEME_UPDATED)
            )
        );
    }

    @Test
    public void shouldGetBackgroundImageUrl() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        when(theme.getBackgroundImage()).thenReturn("http://localhost/image");
        when(theme.getReferenceId()).thenReturn(GraviteeContext.getCurrentEnvironment());
        when(themeRepository.findById(THEME_ID)).thenReturn(Optional.of(theme));
        PictureEntity backgroundImage = themeService.getBackgroundImage(GraviteeContext.getExecutionContext(), THEME_ID);
        assertNotNull(backgroundImage);
        assertTrue(backgroundImage instanceof UrlPictureEntity);
    }

    @Test
    public void shouldGetBackgroundImage() {
        PictureEntity backgroundImage = themeService.getBackgroundImage(GraviteeContext.getExecutionContext(), THEME_ID);
        assertNull(backgroundImage);
    }

    @Test
    public void shouldGetLogo() throws TechnicalException {
        final Theme theme = mock(Theme.class, withSettings().lenient());
        when(theme.getReferenceId()).thenReturn(GraviteeContext.getCurrentEnvironment());
        when(theme.getLogo()).thenReturn(themeServiceImpl.getDefaultLogo());
        when(themeRepository.findById(THEME_ID)).thenReturn(Optional.of(theme));

        PictureEntity logo = themeService.getLogo(GraviteeContext.getExecutionContext(), THEME_ID);
        assertNotNull(logo);
        assertTrue(logo instanceof InlinePictureEntity);
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "favicon", "backgroundImage" })
    void should_reject_active_images_before_creating_a_theme(String field) throws TechnicalException {
        when(themeRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NewThemeEntity theme = new NewThemeEntity();
        theme.setName("Unsafe theme");
        ReflectionTestUtils.setField(theme, field, htmlImage("text/html"));

        assertThrows(ValidationDomainException.class, () -> themeService.createPortalTheme(GraviteeContext.getExecutionContext(), theme));

        verify(themeRepository, never()).create(any());
        verifyNoInteractions(auditService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "favicon", "backgroundImage" })
    void should_reject_unchanged_legacy_images_before_updating_a_theme(String field) throws TechnicalException {
        when(themeRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Theme existing = storedTheme();
        ReflectionTestUtils.setField(existing, field, htmlImage("image/png"));
        when(themeRepository.findById(THEME_ID)).thenReturn(of(existing));
        UpdateThemeEntity update = new UpdateThemeEntity();
        update.setId(THEME_ID);
        ReflectionTestUtils.setField(update, field, htmlImage("image/png"));

        assertThrows(ValidationDomainException.class, () -> themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), update));

        verify(themeRepository, never()).update(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void should_validate_default_images_before_updating_a_theme() throws TechnicalException {
        when(themeRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(themeRepository.findById(THEME_ID)).thenReturn(of(storedTheme()));
        ThemeServiceImpl service = spy((ThemeServiceImpl) themeService);
        doReturn(htmlImage("image/png")).when(service).getDefaultLogo();
        UpdateThemeEntity update = new UpdateThemeEntity();
        update.setId(THEME_ID);

        assertThrows(ValidationDomainException.class, () -> service.updatePortalTheme(GraviteeContext.getExecutionContext(), update));

        verify(themeRepository, never()).update(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void should_preserve_the_previous_theme_when_reset_defaults_are_invalid() throws TechnicalException {
        when(themeRepository.findById(THEME_ID)).thenReturn(of(storedTheme()));
        when(themeRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ThemeServiceImpl service = spy((ThemeServiceImpl) themeService);
        doReturn(htmlImage("image/png")).when(service).getDefaultFavicon();

        assertThrows(ValidationDomainException.class, () -> service.resetToDefaultTheme(GraviteeContext.getExecutionContext(), THEME_ID));

        verify(themeRepository, never()).delete(anyString());
        verify(themeRepository, never()).create(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void should_reuse_an_existing_theme_on_reset_without_loading_default_images() throws TechnicalException {
        Theme previous = storedTheme();
        Theme replacement = storedTheme();
        replacement.setId("other-theme");
        replacement.setEnabled(true);
        when(themeRepository.findById(THEME_ID)).thenReturn(of(previous));
        when(themeRepository.findByReferenceIdAndReferenceTypeAndType("DEFAULT", ENVIRONMENT.name(), ThemeType.PORTAL)).thenReturn(
            Set.of(previous, replacement)
        );
        ThemeServiceImpl service = spy((ThemeServiceImpl) themeService);

        GenericThemeEntity result = service.resetToDefaultTheme(GraviteeContext.getExecutionContext(), THEME_ID);

        assertEquals("other-theme", result.getId());
        verify(themeRepository).delete(THEME_ID);
        verify(themeRepository, never()).create(any());
        verify(service, never()).getDefaultLogo();
        verify(service, never()).getDefaultFavicon();
    }

    @Test
    void should_persist_the_same_default_image_that_was_validated_on_reset() throws TechnicalException {
        when(themeRepository.findById(THEME_ID)).thenReturn(of(storedTheme()));
        when(themeRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ThemeServiceImpl service = spy((ThemeServiceImpl) themeService);
        String validatedImage = themeServiceImpl.getDefaultFavicon();
        doReturn(validatedImage, htmlImage("text/html")).when(service).getDefaultFavicon();

        GenericThemeEntity result = service.resetToDefaultTheme(GraviteeContext.getExecutionContext(), THEME_ID);

        assertEquals(validatedImage, result.getFavicon());
        verify(service).getDefaultFavicon();
        verify(themeRepository).create(argThat(theme -> validatedImage.equals(theme.getFavicon())));
    }

    @Test
    void should_preserve_valid_image_strings_when_creating_a_theme() throws TechnicalException {
        String image = themeServiceImpl.getDefaultLogo();
        NewThemeEntity theme = new NewThemeEntity();
        theme.setName("Valid theme");
        theme.setLogo(image);
        theme.setOptionalLogo("");
        theme.setFavicon("https://example.test/favicon.ico");
        when(themeRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThemeEntity result = themeService.createPortalTheme(GraviteeContext.getExecutionContext(), theme);

        assertEquals(image, result.getLogo());
        assertEquals("", result.getOptionalLogo());
        assertEquals("https://example.test/favicon.ico", result.getFavicon());
        assertNull(result.getBackgroundImage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "favicon", "backgroundImage" })
    void should_reject_active_legacy_images_on_read_without_loading_defaults(String field) throws TechnicalException {
        Theme existing = storedTheme();
        ReflectionTestUtils.setField(existing, field, htmlImage("text/html"));
        when(themeRepository.findById(THEME_ID)).thenReturn(of(existing));
        ThemeServiceImpl service = spy((ThemeServiceImpl) themeService);

        assertThrows(ValidationDomainException.class, () -> readImage(service, field));

        verify(service, never()).getDefaultLogo();
        verify(service, never()).getDefaultOptionalLogo();
        verify(service, never()).getDefaultFavicon();
        verify(service, never()).getDefaultBackgroundImage();
    }

    @Test
    void should_keep_passive_content_type_for_unverified_legacy_bytes_on_read() throws TechnicalException {
        Theme existing = storedTheme();
        existing.setFavicon(htmlImage("image/png"));
        when(themeRepository.findById(THEME_ID)).thenReturn(of(existing));

        InlinePictureEntity result = (InlinePictureEntity) themeService.getFavicon(GraviteeContext.getExecutionContext(), THEME_ID);

        assertEquals("image/png", result.getType());
        assertArrayEquals("<!doctype html><html>not an image</html>".getBytes(StandardCharsets.UTF_8), result.getContent());
    }

    @Test
    void should_allow_deleting_a_theme_with_invalid_legacy_images() throws TechnicalException {
        Theme existing = storedTheme();
        existing.setFavicon(htmlImage("text/html"));
        when(themeRepository.findById(THEME_ID)).thenReturn(of(existing));

        themeService.delete(GraviteeContext.getExecutionContext(), THEME_ID);

        verify(themeRepository).delete(THEME_ID);
    }

    private Theme storedTheme() {
        Theme theme = new Theme();
        theme.setId(THEME_ID);
        theme.setName("Existing theme");
        theme.setType(ThemeType.PORTAL);
        theme.setDefinition("{}");
        theme.setReferenceId("DEFAULT");
        theme.setReferenceType(ENVIRONMENT.name());
        return theme;
    }

    private String htmlImage(String mimeType) {
        return (
            "data:" +
            mimeType +
            ";base64," +
            Base64.getEncoder().encodeToString("<!doctype html><html>not an image</html>".getBytes(StandardCharsets.UTF_8))
        );
    }

    private PictureEntity readImage(ThemeService service, String field) {
        return switch (field) {
            case "logo" -> service.getLogo(GraviteeContext.getExecutionContext(), THEME_ID);
            case "optionalLogo" -> service.getOptionalLogo(GraviteeContext.getExecutionContext(), THEME_ID);
            case "favicon" -> service.getFavicon(GraviteeContext.getExecutionContext(), THEME_ID);
            case "backgroundImage" -> service.getBackgroundImage(GraviteeContext.getExecutionContext(), THEME_ID);
            default -> throw new IllegalArgumentException(field);
        };
    }
}
