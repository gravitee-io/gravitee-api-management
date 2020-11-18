/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.theme.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DuplicateThemeNameException;
import io.gravitee.rest.api.service.exceptions.ThemeNotFoundException;
import io.gravitee.rest.api.service.impl.ThemeServiceImpl;
import io.gravitee.rest.api.service.impl.ThemeServiceImpl.ThemeDefinitionMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static io.gravitee.repository.management.model.Audit.AuditProperties.THEME;
import static io.gravitee.repository.management.model.ThemeReferenceType.ENVIRONMENT;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ThemeServiceTest {

    private static final String THEME_ID = "default";
    private static final String THEMES_PATH = "src/test/resources/themes";

    @InjectMocks
    private ThemeService themeService = new ThemeServiceImpl();

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private AuditService auditService;

    private ThemeServiceImpl themeServiceImpl = new ThemeServiceImpl();

    @Before
    public void init() {
        ReflectionTestUtils.setField(themeService, "themesPath", THEMES_PATH);
        ReflectionTestUtils.setField(themeServiceImpl, "themesPath", THEMES_PATH);
    }

    @Test
    public void shouldFindById() throws TechnicalException, JsonProcessingException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(Collections.EMPTY_LIST);
        String definition = definitionMapper.writeValueAsString(themeDefinition);

        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.getDefinition()).thenReturn(definition);
        when(theme.getReferenceId()).thenReturn("DEFAULT");
        when(theme.getCreatedAt()).thenReturn(new Date(1));
        when(theme.getUpdatedAt()).thenReturn(new Date(2));
        when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));

        final ThemeEntity themeEntity = themeService.findById(THEME_ID);
        assertEquals(THEME_ID, themeEntity.getId());
        assertEquals("NAME", themeEntity.getName());
        assertEquals(definition, definitionMapper.writeValueAsString(themeEntity.getDefinition()));
        assertEquals(new Date(1), themeEntity.getCreatedAt());
        assertEquals(new Date(2), themeEntity.getUpdatedAt());
    }

    @Test(expected = ThemeNotFoundException.class)
    public void shouldThrowThemeNotFoundException() throws TechnicalException {
        when(themeRepository.findById(THEME_ID)).thenReturn(empty());
        themeService.findById(THEME_ID);
    }

    @Test(expected = ThemeNotFoundException.class)
    public void shouldThrowThemeNotFoundExceptionWhenThemeIsNotInDefaultEnv() throws TechnicalException, JsonProcessingException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(Collections.EMPTY_LIST);
        String definition = definitionMapper.writeValueAsString(themeDefinition);

        final Theme theme = mock(Theme.class);
        when(theme.getReferenceId()).thenReturn("NOT-DEFAULT");
        when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));

        final ThemeEntity themeEntity = themeService.findById(THEME_ID);
        assertEquals(THEME_ID, themeEntity.getId());
        assertEquals("NAME", themeEntity.getName());
        assertEquals(definition, definitionMapper.writeValueAsString(themeEntity.getDefinition()));
        assertEquals(new Date(1), themeEntity.getCreatedAt());
        assertEquals(new Date(2), themeEntity.getUpdatedAt());
    }

    @Test
    public void shouldFindAll() throws TechnicalException, JsonProcessingException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        String definition = themeServiceImpl.getDefaultDefinition();
        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.getDefinition()).thenReturn(definition);
        when(theme.getCreatedAt()).thenReturn(new Date(1));
        when(theme.getUpdatedAt()).thenReturn(new Date(2));
        when(themeRepository.findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ENVIRONMENT.name())).thenReturn(singleton(theme));

        final Set<ThemeEntity> themes = themeService.findAll();
        final ThemeEntity themeEntity = themes.iterator().next();
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
        when(themeRepository.findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ENVIRONMENT.name())).thenReturn(singleton(theme));

        assertNotNull(themeService.findEnabled());
    }

    @Test
    public void shouldGetDefaultIfNoThemeEnabled() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        when(theme.isEnabled()).thenReturn(false);
        when(themeRepository.findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ENVIRONMENT.name())).thenReturn(singleton(theme));

        assertNotNull(themeService.findEnabled());
    }

    @Test
    public void shouldCreate() throws TechnicalException, IOException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(Collections.EMPTY_LIST);
        String definition = definitionMapper.writeValueAsString(themeDefinition);
        final NewThemeEntity newThemeEntity = new NewThemeEntity();
        newThemeEntity.setName("NAME");
        newThemeEntity.setDefinition(themeDefinition);

        final Theme createdTheme = new Theme();
        createdTheme.setId(THEME_ID);
        createdTheme.setName("NAME");
        createdTheme.setDefinition(definition);
        createdTheme.setCreatedAt(new Date());
        createdTheme.setUpdatedAt(new Date());
        when(themeRepository.create(any())).thenReturn(createdTheme);

        final ThemeEntity themeEntity = themeService.create(newThemeEntity);

        assertNotNull(themeEntity.getId());
        assertEquals("NAME", themeEntity.getName());
        assertNotNull(themeEntity.getDefinition());
        assertEquals(0, themeEntity.getDefinition().getData().size());
        assertNotNull(themeEntity.getCreatedAt());
        assertNotNull(themeEntity.getUpdatedAt());

        final Theme theme = new Theme();
        theme.setName("NAME");
        theme.setDefinition(definition);
        theme.setReferenceId("REF_ID");
        theme.setReferenceType(ENVIRONMENT.name());

        verify(themeRepository, times(1)).create(argThat(argument ->
        {
            return "NAME".equals(argument.getName()) &&
                    argument.getDefinition() != null &&
                    "DEFAULT".equals(argument.getReferenceId()) &&
                    ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                    !argument.getId().isEmpty() &&
                    argument.getCreatedAt() != null &&
                    argument.getUpdatedAt() != null;
        }));
        verify(auditService, times(1)).createEnvironmentAuditLog(
                eq(ImmutableMap.of(THEME, THEME_ID)),
                eq(Theme.AuditEvent.THEME_CREATED),
                any(Date.class),
                isNull(),
                any());
    }

    @Test(expected = DuplicateThemeNameException.class)
    public void shouldThrowDuplicateThemeNameExceptionOnCreate() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.getDefinition()).thenReturn(themeServiceImpl.getDefaultDefinition());
        when(themeRepository.findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ENVIRONMENT.name())).thenReturn(singleton(theme));

        final NewThemeEntity newThemeEntity = new NewThemeEntity();
        newThemeEntity.setName("NAME");
        themeService.create(newThemeEntity);
    }

    @Test
    public void shouldUpdate() throws TechnicalException, JsonProcessingException {
        ThemeDefinitionMapper definitionMapper = new ThemeDefinitionMapper();
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(Collections.EMPTY_LIST);
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
        when(themeRepository.update(any())).thenReturn(updatedTheme);
        when(themeRepository.findById(THEME_ID)).thenReturn(of(updatedTheme));

        final ThemeEntity themeEntity = themeService.update(updateThemeEntity);

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

        verify(themeRepository, times(1)).update(argThat(argument ->
                "NAME".equals(argument.getName()) &&
                        argument.getDefinition() != null &&
                        "DEFAULT".equals(argument.getReferenceId()) &&
                        ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                        THEME_ID.equals(argument.getId()) &&
                        argument.getUpdatedAt() != null));

        verify(auditService, times(1)).createEnvironmentAuditLog(
                eq(ImmutableMap.of(THEME, THEME_ID)),
                eq(Theme.AuditEvent.THEME_UPDATED),
                any(Date.class),
                any(),
                any());
    }

    @Test(expected = DuplicateThemeNameException.class)
    public void shouldThrowDuplicateThemeNameExceptionOnUpdate() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.getDefinition()).thenReturn(themeServiceImpl.getDefaultDefinition());

        final Theme theme2 = mock(Theme.class);
        when(theme2.getId()).thenReturn("foobar");
        when(theme2.getName()).thenReturn("NAME");
        when(theme2.getDefinition()).thenReturn(themeServiceImpl.getDefaultDefinition());

        when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));
        when(themeRepository.findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ENVIRONMENT.name())).thenReturn(new HashSet(asList(theme, theme2)));

        final UpdateThemeEntity updateThemeEntity = new UpdateThemeEntity();
        updateThemeEntity.setId(THEME_ID);
        updateThemeEntity.setName("NAME");
        themeService.update(updateThemeEntity);
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

        themeService.update(updateThemeEntity);
        verify(themeRepository).create(any());
    }

    @Test
    public void shouldResetToDefaultTheme() throws TechnicalException, JsonProcessingException {
        ThemeDefinition themeDefinition = new ThemeDefinition();
        themeDefinition.setData(Collections.EMPTY_LIST);

        final Theme theme = new Theme();
        theme.setId(THEME_ID);
        theme.setName("NAME");
        theme.setDefinition(themeServiceImpl.getDefinition(THEMES_PATH + "/custom-definition.json"));
        theme.setReferenceId("DEFAULT");
        theme.setReferenceType(ENVIRONMENT.name());
        theme.setCreatedAt(new Date());
        theme.setUpdatedAt(new Date());

        themeService.resetToDefaultTheme(THEME_ID);

        verify(themeRepository, times(1)).delete(THEME_ID);

        verify(auditService, times(1)).createEnvironmentAuditLog(
                eq(ImmutableMap.of(THEME, THEME_ID)),
                eq(Theme.AuditEvent.THEME_RESET),
                any(Date.class),
                any(),
                any());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        when(themeRepository.findById(THEME_ID)).thenReturn(of(theme));

        themeService.delete(THEME_ID);

        verify(themeRepository, times(1)).delete(THEME_ID);
        verify(auditService, times(1)).createEnvironmentAuditLog(
                eq(ImmutableMap.of(THEME, THEME_ID)),
                eq(Theme.AuditEvent.THEME_DELETED),
                any(Date.class),
                isNull(),
                eq(theme));
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
    public void shouldMergeThemeDefinition() throws IOException, TechnicalException {
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

        ThemeDefinition themeDefinition = definitionMapper.readDefinition(definition);
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
        themeToCreate.setDefinition(definitionMapper.readDefinition(definition));

        final Theme createdTheme = new Theme();
        createdTheme.setId(THEME_ID);
        createdTheme.setName("Default");
        createdTheme.setDefinition(definition);
        createdTheme.setCreatedAt(new Date());
        createdTheme.setUpdatedAt(new Date());
        when(themeRepository.create(any())).thenReturn(createdTheme);
        assertEquals(definitionMapper.readTree(definition), definitionMapper.readTree(definition));
        assertEquals(definition, definition);

        themeService.update(themeToCreate);

        verify(themeRepository, times(1)).create(argThat(argument ->
        {
            try {
                return "Default".equals(argument.getName()) &&
                        definitionMapper.readTree(argument.getDefinition()).equals(definitionMapper.readTree(definition)) &&
                        "DEFAULT".equals(argument.getReferenceId()) &&
                        ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                        !argument.getId().isEmpty() &&
                        argument.getCreatedAt() != null &&
                        argument.getUpdatedAt() != null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }));
        verify(auditService, times(1)).createEnvironmentAuditLog(
                eq(ImmutableMap.of(THEME, THEME_ID)),
                eq(Theme.AuditEvent.THEME_CREATED),
                any(Date.class),
                isNull(),
                any());

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

        when(themeRepository.findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ENVIRONMENT.name())).thenReturn(new HashSet(asList(theme, theme2)));

        String mergeDefinition = themeDefinitionMapper.writeValueAsString(themeDefinitionMapper.merge(definition, customDefinition));

        themeService.updateDefaultTheme();

        verify(themeRepository, times(1)).update(argThat(argument ->
        {
            try {
                return "NAME".equals(argument.getName()) &&
                        mapper.readTree(argument.getDefinition()).equals(mapper.readTree(mergeDefinition)) &&
                        "DEFAULT".equals(argument.getReferenceId()) &&
                        ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                        !argument.getId().isEmpty() &&
                        argument.getCreatedAt() != null &&
                        argument.getUpdatedAt() != null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }));
        verify(auditService, times(1)).createEnvironmentAuditLog(
                eq(ImmutableMap.of(THEME, THEME_ID)),
                eq(Theme.AuditEvent.THEME_UPDATED),
                any(Date.class),
                any(),
                any());

    }

    @Test
    public void shouldGetBackgroundImageUrl() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        when(theme.getBackgroundImage()).thenReturn("http://localhost/image");
        when(theme.getId()).thenReturn(THEME_ID);
        when(theme.getName()).thenReturn("NAME");
        when(theme.isEnabled()).thenReturn(true);
        when(theme.getDefinition()).thenReturn(themeServiceImpl.getDefaultDefinition());
        when(themeRepository.findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ENVIRONMENT.name())).thenReturn(singleton(theme));
        PictureEntity backgroundImage = themeService.getBackgroundImage(THEME_ID);
        assertNotNull(backgroundImage);
        assertTrue(backgroundImage instanceof UrlPictureEntity);
    }

    @Test
    public void shouldGetBackgroundImage() throws TechnicalException {
        final Theme theme = mock(Theme.class);
        Mockito.lenient().when(theme.getReferenceType()).thenReturn(ENVIRONMENT.name());
        PictureEntity backgroundImage = themeService.getBackgroundImage(THEME_ID);
        assertNull(backgroundImage);
    }

    @Test
    public void shouldGetLogo() throws TechnicalException {
        final Theme theme = mock(Theme.class, withSettings().lenient());
        Mockito.lenient().when(theme.getReferenceType()).thenReturn(ENVIRONMENT.name());
        when(theme.getReferenceId()).thenReturn("DEFAULT");
        when(theme.getLogo()).thenReturn(themeServiceImpl.getDefaultLogo());

        PictureEntity logo = themeService.getLogo(THEME_ID);
        assertNotNull(logo);
        assertTrue(logo instanceof InlinePictureEntity);
    }

}
