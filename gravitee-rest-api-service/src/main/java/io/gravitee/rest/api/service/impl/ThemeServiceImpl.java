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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.management.model.ThemeReferenceType;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.theme.*;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DuplicateThemeNameException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.ThemeNotFoundException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.THEME;
import static io.gravitee.repository.management.model.Theme.AuditEvent.*;
import static java.nio.charset.Charset.defaultCharset;

/**
 * @author Guillaume CUSNIEUX (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ThemeServiceImpl extends AbstractService implements ThemeService {

    private final Logger LOGGER = LoggerFactory.getLogger(ThemeServiceImpl.class);
    private static final ThemeDefinitionMapper MAPPER = new ThemeDefinitionMapper();
    private static final String DEFAULT_THEME_PATH = "/themes/default/definition.json";
    private static final String DEFAULT_THEME_ID = "default";


    @Autowired
    private ThemeRepository themeRepository;
    @Autowired
    private AuditService auditService;

    @Override
    public Set<ThemeEntity> findAll() {
        try {
            LOGGER.debug("Find all themes by reference: " + GraviteeContext.getCurrentEnvironment());
            return themeRepository
                    .findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ThemeReferenceType.ENVIRONMENT.name())
                    .stream()
                    .map(this::convert).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all themes", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all themes", ex);
        }
    }

    @Override
    public ThemeEntity findById(String themeId) {
        return convert(this.findByIdWithoutConvert(themeId));
    }

    private Theme findByIdWithoutConvert(String themeId) {
        try {
            LOGGER.debug("Find theme by ID: {}", themeId);
            Optional<Theme> optTheme = themeRepository.findById(themeId);

            if (!optTheme.isPresent()) {
                throw new ThemeNotFoundException(themeId);
            }

            Theme theme = optTheme.get();
            if (!theme.getReferenceId().equals(GraviteeContext.getCurrentEnvironment())) {
                LOGGER.warn("Theme is not in current environment " +
                        GraviteeContext.getCurrentEnvironment() +
                        " actual:" + theme.getReferenceId());
                throw new ThemeNotFoundException(themeId);
            }
            return theme;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find theme by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find theme by ID", ex);
        }
    }

    @Override
    public ThemeEntity create(final NewThemeEntity themeEntity) {
        // First we prevent the duplicate name
        try {

            if (this.findByName(themeEntity.getName(), null).isPresent()) {
                throw new DuplicateThemeNameException(themeEntity.getName());
            }

            Theme theme = themeRepository.create(convert(themeEntity));

            auditService.createEnvironmentAuditLog(
                    Collections.singletonMap(THEME, theme.getId()),
                    THEME_CREATED,
                    theme.getCreatedAt(),
                    null,
                    theme);

            return convert(theme);

        } catch (TechnicalException ex) {
            final String error = "An error occurred while trying to create theme " + themeEntity;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private Optional<ThemeEntity> findByName(String name, String excludedId) {
        return findAll()
                .stream()
                .filter(t -> !t.getId().equals(excludedId) && t.getName().equals(name))
                .findAny();

    }

    @Override
    public ThemeEntity update(final UpdateThemeEntity updateThemeEntity) {
        try {
            final Optional<Theme> themeOptional = themeRepository.findById(updateThemeEntity.getId());
            if (themeOptional.isPresent()) {

                final Theme theme = new Theme(themeOptional.get());
                if (this.findByName(theme.getName(), theme.getId()).isPresent()) {
                    throw new DuplicateThemeNameException(theme.getName());
                }

                theme.setEnabled(updateThemeEntity.isEnabled());
                final Date now = new Date();
                theme.setUpdatedAt(now);
                theme.setReferenceType(ThemeReferenceType.ENVIRONMENT.name());
                theme.setReferenceId(GraviteeContext.getCurrentEnvironment());

                if (updateThemeEntity.getName() != null) {
                    theme.setName(updateThemeEntity.getName());
                }

                theme.setDefinition(MAPPER.writeValueAsString(updateThemeEntity.getDefinition()));

                if (updateThemeEntity.getLogo() != null) {
                    theme.setLogo(updateThemeEntity.getLogo());
                } else {
                    theme.setLogo(this.getDefaultLogo());
                }

                theme.setBackgroundImage(updateThemeEntity.getBackgroundImage());

                if (updateThemeEntity.getOptionalLogo() != null) {
                    theme.setOptionalLogo(updateThemeEntity.getOptionalLogo());
                } else {
                    theme.setOptionalLogo(this.getDefaultOptionalLogo());
                }

                final ThemeEntity savedTheme = convert(themeRepository.update(theme));
                auditService.createEnvironmentAuditLog(
                        Collections.singletonMap(THEME, theme.getId()),
                        THEME_UPDATED,
                        new Date(),
                        themeOptional.get(),
                        theme);
                return savedTheme;
            } else {
                final NewThemeEntity newTheme = new NewThemeEntity();
                newTheme.setName(updateThemeEntity.getName());
                newTheme.setDefinition(updateThemeEntity.getDefinition());
                newTheme.setBackgroundImage(updateThemeEntity.getBackgroundImage());
                newTheme.setLogo(updateThemeEntity.getLogo());
                newTheme.setOptionalLogo(updateThemeEntity.getOptionalLogo());
                newTheme.setEnabled(updateThemeEntity.isEnabled());
                return create(newTheme);
            }
        } catch (TechnicalException | JsonProcessingException ex) {
            final String error = "An error occurred while trying to update theme " + updateThemeEntity;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public void delete(String themeId) {
        try {
            Optional<Theme> themeOptional = themeRepository.findById(themeId);
            if (themeOptional.isPresent()) {
                themeRepository.delete(themeId);
                auditService.createEnvironmentAuditLog(
                        Collections.singletonMap(THEME, themeId),
                        THEME_DELETED,
                        new Date(),
                        null,
                        themeOptional.get());
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to delete theme " + themeId;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public ThemeEntity findEnabled() {
        try {
            LOGGER.debug("Find all themes by reference type");
            Optional<Theme> themeEnabled = themeRepository.findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ThemeReferenceType.ENVIRONMENT.name())
                    .stream()
                    .filter(theme -> theme.isEnabled())
                    .findFirst();

            if (themeEnabled.isPresent()) {
                return convert(themeEnabled.get());
            }

            final ThemeEntity theme = new ThemeEntity();
            theme.setId(DEFAULT_THEME_ID);
            theme.setName("Default theme");
            theme.setDefinition(MAPPER.readDefinition(getDefaultDefinition()));
            theme.setLogo(this.getDefaultLogo());
            theme.setOptionalLogo(this.getDefaultOptionalLogo());
            return theme;

        } catch (IOException ex) {
            final String error = "Error while trying to get the default theme";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find all themes by reference type";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public void updateDefaultTheme() {
        try {
            final Set<Theme> themes = themeRepository
                    .findByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), ThemeReferenceType.ENVIRONMENT.name());

            String defaultDefinition = this.getDefaultDefinition();
            if (themes != null && !themes.isEmpty()) {
                themes.forEach(theme -> {
                    if (!MAPPER.isSame(defaultDefinition, theme.getDefinition())) {
                        try {
                            ThemeDefinition mergeDefinition = MAPPER.merge(defaultDefinition, theme.getDefinition());
                            Theme themeUpdate = new Theme(theme);
                            themeUpdate.setDefinition(MAPPER.writeValueAsString(mergeDefinition));
                            theme.setUpdatedAt(new Date());
                            this.themeRepository.update(themeUpdate);
                            auditService.createEnvironmentAuditLog(
                                    Collections.singletonMap(THEME, theme.getId()),
                                    THEME_UPDATED,
                                    new Date(),
                                    theme,
                                    themeUpdate);
                        } catch (IOException ex) {
                            final String error = "Error while trying to merge default theme from the definition path: "
                                    + DEFAULT_THEME_PATH
                                    + " with theme "
                                    + theme.toString();
                            LOGGER.error(error, ex);
                        } catch (TechnicalException ex) {
                            final String error = "Error while trying to update theme after merge with default" + theme.toString();
                            LOGGER.error(error, ex);
                        }
                    }
                });
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all themes", ex);
        }
    }

    public String getDefaultDefinition() {
        return this.getDefinition(DEFAULT_THEME_PATH);
    }

    public String getDefinition(String path) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = IOUtils.toString(this.getClass().getResourceAsStream(path), defaultCharset());
            // Important for remove formatting (space, line break...)
            JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
            return jsonNode.toString();
        } catch (IOException ex) {
            final String error = "Error while trying to load a theme from the definition path: " + path;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    public String getDefaultLogo() {
        return getImage("logo.png");
    }

    public String getDefaultOptionalLogo() {
        return getImage("logo-light.png");
    }

    private String getImage(String filename) {
        String filepath = "/themes/default/" + filename;
        try {
            byte[] image = IOUtils.toByteArray(this.getClass().getResourceAsStream(filepath));
            MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
            return "data:" + fileTypeMap.getContentType(filename) + ";base64," + Base64.getEncoder().encodeToString(image);
        } catch (IOException ex) {
            final String error = "Error while trying to load image from: " + filepath;
            LOGGER.error(error, ex);
            return null;
        }
    }

    @Override
    public ThemeEntity resetToDefaultTheme(String themeId) {
        try {
            LOGGER.debug("Reset to default theme by ID: {}", themeId);
            final ThemeEntity previousTheme = findEnabled();
            themeRepository.delete(DEFAULT_THEME_ID);
            auditService.createEnvironmentAuditLog(
                    Collections.singletonMap(THEME, themeId),
                    THEME_RESET,
                    new Date(),
                    previousTheme,
                    null);
            return findEnabled();
        } catch (Exception ex) {
            final String error = "Error while trying to reset a default theme";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public PictureEntity getLogo(String themeId) {
        try {
            final String logo = findEnabled().getLogo();
            if (logo != null) {
                return convertToPicture(logo);
            }
        } catch (Exception ex) {
            LOGGER.warn("Unable to get logo picture theme for id[{}]", themeId);
        }
        return null;
    }

    @Override
    public PictureEntity getOptionalLogo(String themeId) {
        try {
            final String optionalLogo = findEnabled().getOptionalLogo();
            if (optionalLogo != null) {
                return convertToPicture(optionalLogo);
            }
        } catch (Exception ex) {
            LOGGER.warn("Unable to get optional logo theme for id[{}]", themeId);
        }
        return null;
    }

    @Override
    public PictureEntity getBackgroundImage(String themeId) {
        try {
            final String backgroundImage = findEnabled().getBackgroundImage();
            if (backgroundImage != null) {
                return convertToPicture(backgroundImage);
            }
        } catch (Exception ex) {
            LOGGER.warn("Unable to get background image theme for id[{}]", themeId);
        }
        return null;
    }

    private PictureEntity convertToPicture(String picture) {
        if (picture.matches("^(http|https)://.*$")) {
            return new UrlPictureEntity(picture);
        } else {
            InlinePictureEntity imageEntity = new InlinePictureEntity();
            String[] parts = picture.split(";", 2);
            imageEntity.setType(parts[0].split(":")[1]);
            String base64Content = picture.split(",", 2)[1];
            imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
            return imageEntity;
        }
    }

    private Theme convert(NewThemeEntity themeEntity) {
        try {
            final Date now = new Date();
            final Theme theme = new Theme();
            theme.setId(DEFAULT_THEME_ID);
            theme.setCreatedAt(now);
            theme.setUpdatedAt(now);
            theme.setReferenceId(GraviteeContext.getCurrentEnvironment());
            theme.setReferenceType(ThemeReferenceType.ENVIRONMENT.name());
            theme.setLogo(themeEntity.getLogo());
            theme.setName(themeEntity.getName());
            theme.setDefinition(MAPPER.writeValueAsString(themeEntity.getDefinition()));
            theme.setEnabled(themeEntity.isEnabled());
            theme.setBackgroundImage(themeEntity.getBackgroundImage());
            theme.setOptionalLogo(themeEntity.getOptionalLogo());
            return theme;
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException("Cannot convert new theme entity", e);
        }
    }

    private ThemeEntity convert(final Theme theme) {
        final ThemeEntity themeEntity = new ThemeEntity();
        themeEntity.setId(theme.getId());
        themeEntity.setName(theme.getName());
        try {
            themeEntity.setDefinition(MAPPER.readDefinition(theme.getDefinition()));
        } catch (IOException e) {
            LOGGER.error("Cannot read definition of theme " + theme.getId() + " definition:" + theme.getDefinition());
        }
        themeEntity.setCreatedAt(theme.getCreatedAt());
        themeEntity.setUpdatedAt(theme.getUpdatedAt());
        themeEntity.setEnabled(theme.isEnabled());
        themeEntity.setLogo(theme.getLogo());
        themeEntity.setBackgroundImage(theme.getBackgroundImage());
        themeEntity.setOptionalLogo(theme.getOptionalLogo());
        return themeEntity;
    }


    public static class ThemeDefinitionMapper extends ObjectMapper {

        private final Logger LOGGER = LoggerFactory.getLogger(ThemeDefinitionMapper.class);

        public ThemeDefinition readDefinition(String themeDefinition) throws IOException {
            return this.readValue(themeDefinition, ThemeDefinition.class);
        }

        public ThemeDefinition merge(final String base, final String override) throws IOException {
            final ThemeDefinition overrideDefinition = this.readDefinition(override);
            final ThemeDefinition overrideDefinitionFinal = this.readDefinition(override);

            ThemeDefinition mergedDefinition = this.readerForUpdating(overrideDefinition).readValue(base);

            List<ThemeComponentDefinition> componentsData = mergedDefinition
                    .getData()
                    .stream()
                    .map(component -> {

                        List<ThemeCssDefinition> cssMerged = component.getCss()
                                .stream()
                                .map(css -> {
                                    ThemeCssDefinition customCss = getThemeCssDefinition(overrideDefinitionFinal, component.getName(), css.getName());
                                    if (customCss != null) {
                                        css.setValue(customCss.getValue());
                                    }
                                    return css;
                                })
                                .collect(Collectors.toList());
                        component.setCss(cssMerged);
                        return component;
                    })
                    .collect(Collectors.toList());

            mergedDefinition.setData(componentsData);
            return mergedDefinition;
        }

        public ThemeComponentDefinition getThemeComponentDefinition(ThemeDefinition themeDefinition, String name) {
            return themeDefinition.getData()
                    .stream()
                    .filter(themeComponentDefinition -> name.equals(themeComponentDefinition.getName()))
                    .findFirst()
                    .orElse(null);
        }

        public ThemeCssDefinition getThemeCssDefinition(ThemeDefinition themeDefinition, String name, String cssName) {
            ThemeComponentDefinition componentDefinition = getThemeComponentDefinition(themeDefinition, name);
            if (componentDefinition != null) {
                return componentDefinition.getCss()
                        .stream()
                        .filter(css -> cssName.equals(css.getName())).findFirst().orElse(null);
            }
            return null;
        }

        public boolean isSame(String definitionA, String definitionB) {
            try {
                return this.readTree(definitionA).equals(this.readTree(definitionB));
            } catch (IOException e) {
                LOGGER.error("Cannot compare definition " + definitionA + " and " + definitionB, e);
            }
            return false;
        }
    }

}
