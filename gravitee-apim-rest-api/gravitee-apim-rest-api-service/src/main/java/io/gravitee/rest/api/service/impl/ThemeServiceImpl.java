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
import static io.gravitee.repository.management.model.Theme.AuditEvent.*;
import static io.gravitee.repository.management.model.ThemeReferenceType.ENVIRONMENT;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.management.model.ThemeType;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.theme.GenericThemeEntity;
import io.gravitee.rest.api.model.theme.portal.NewThemeEntity;
import io.gravitee.rest.api.model.theme.portal.ThemeComponentDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeCssDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.model.theme.portal.UpdateThemeEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DuplicateThemeNameException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.ThemeNotFoundException;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ThemeServiceImpl extends AbstractService implements ThemeService {

    private final Logger LOGGER = LoggerFactory.getLogger(ThemeServiceImpl.class);
    private static final ThemeDefinitionMapper MAPPER = new ThemeDefinitionMapper();
    private static final String DEFAULT_THEME_PATH = "/definition.json";

    @Lazy
    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private AuditService auditService;

    @Value("${portal.themes.path:${gravitee.home}/themes}")
    private String themesPath;

    @Override
    public Set<GenericThemeEntity> findAllByType(final ExecutionContext executionContext, io.gravitee.rest.api.model.theme.ThemeType type) {
        try {
            LOGGER.debug("Find all themes by reference: " + executionContext.getEnvironmentId());
            return themeRepository
                .findByReferenceIdAndReferenceTypeAndType(
                    executionContext.getEnvironmentId(),
                    ENVIRONMENT.name(),
                    ThemeType.valueOf(type.name())
                )
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all themes", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all themes", ex);
        }
    }

    @Override
    public GenericThemeEntity findById(final ExecutionContext executionContext, String themeId) {
        return convert(this.findByIdWithoutConvert(executionContext, themeId));
    }

    private Theme findByIdWithoutConvert(final ExecutionContext executionContext, String themeId) {
        try {
            LOGGER.debug("Find theme by ID: {}", themeId);
            Optional<Theme> optTheme = themeRepository.findById(themeId);

            if (!optTheme.isPresent()) {
                throw new ThemeNotFoundException(themeId);
            }

            Theme theme = optTheme.get();
            if (!theme.getReferenceId().equals(executionContext.getEnvironmentId())) {
                LOGGER.warn(
                    "Theme is not in current environment " + executionContext.getEnvironmentId() + " actual:" + theme.getReferenceId()
                );
                throw new ThemeNotFoundException(themeId);
            }
            return theme;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find theme by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find theme by ID", ex);
        }
    }

    /**
     * Create a new theme of type PORTAL
     *
     * @param executionContext
     * @param themeEntity
     * @return
     */
    @Override
    public ThemeEntity createPortalTheme(final ExecutionContext executionContext, final NewThemeEntity themeEntity) {
        // First we prevent the duplicate name
        try {
            if (this.findByName(executionContext, themeEntity.getName(), null).isPresent()) {
                throw new DuplicateThemeNameException(themeEntity.getName());
            }

            Theme theme = themeRepository.create(convert(executionContext, themeEntity));

            auditService.createAuditLog(
                executionContext,
                Collections.singletonMap(THEME, theme.getId()),
                THEME_CREATED,
                theme.getCreatedAt(),
                null,
                theme
            );

            return convertToPortalThemeEntity(theme);
        } catch (TechnicalException ex) {
            final String error = "An error occurred while trying to create theme " + themeEntity;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private Optional<GenericThemeEntity> findByName(final ExecutionContext executionContext, String name, String excludedId) {
        return findAllByType(executionContext, io.gravitee.rest.api.model.theme.ThemeType.PORTAL)
            .stream()
            .filter(t -> !t.getId().equals(excludedId) && t.getName().equals(name))
            .findAny();
    }

    /**
     * Update a theme of type PORTAL
     *
     * @param executionContext
     * @param updateThemeEntity
     * @return Updated PORTAL theme
     */
    @Override
    public ThemeEntity updatePortalTheme(final ExecutionContext executionContext, final UpdateThemeEntity updateThemeEntity) {
        try {
            final Optional<Theme> themeOptional = themeRepository.findById(updateThemeEntity.getId());
            if (themeOptional.isPresent()) {
                final Theme theme = new Theme(themeOptional.get());

                if (!theme.getReferenceId().equals(executionContext.getEnvironmentId())) {
                    LOGGER.warn(
                        "Theme is not in current environment " + executionContext.getEnvironmentId() + " actual:" + theme.getReferenceId()
                    );
                    throw new ThemeNotFoundException(theme.getId());
                }

                if (this.findByName(executionContext, theme.getName(), theme.getId()).isPresent()) {
                    throw new DuplicateThemeNameException(theme.getName());
                }

                theme.setEnabled(updateThemeEntity.isEnabled());
                final Date now = new Date();
                theme.setUpdatedAt(now);
                theme.setReferenceType(ENVIRONMENT.name());
                theme.setReferenceId(executionContext.getEnvironmentId());

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

                if (updateThemeEntity.getFavicon() != null) {
                    theme.setFavicon(updateThemeEntity.getFavicon());
                } else {
                    theme.setFavicon(this.getDefaultFavicon());
                }

                final ThemeEntity savedTheme = convertToPortalThemeEntity(themeRepository.update(theme));
                auditService.createAuditLog(
                    executionContext,
                    Collections.singletonMap(THEME, theme.getId()),
                    THEME_UPDATED,
                    new Date(),
                    themeOptional.get(),
                    theme
                );
                return savedTheme;
            } else {
                final NewThemeEntity newTheme = new NewThemeEntity();
                newTheme.setName(updateThemeEntity.getName());
                newTheme.setDefinition(updateThemeEntity.getDefinition());
                newTheme.setBackgroundImage(updateThemeEntity.getBackgroundImage());
                newTheme.setLogo(updateThemeEntity.getLogo());
                newTheme.setOptionalLogo(updateThemeEntity.getOptionalLogo());
                newTheme.setFavicon(updateThemeEntity.getFavicon());
                newTheme.setEnabled(updateThemeEntity.isEnabled());
                return createPortalTheme(executionContext, newTheme);
            }
        } catch (TechnicalException | JsonProcessingException ex) {
            final String error = "An error occurred while trying to update theme " + updateThemeEntity;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public void delete(final ExecutionContext executionContext, String themeId) {
        try {
            Optional<Theme> themeOptional = themeRepository
                .findById(themeId)
                .filter(t ->
                    ENVIRONMENT.name().equalsIgnoreCase(t.getReferenceType()) &&
                    t.getReferenceId().equalsIgnoreCase(executionContext.getEnvironmentId())
                );
            if (themeOptional.isPresent()) {
                themeRepository.delete(themeId);
                auditService.createAuditLog(
                    executionContext,
                    Collections.singletonMap(THEME, themeId),
                    THEME_DELETED,
                    new Date(),
                    null,
                    themeOptional.get()
                );
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to delete theme " + themeId;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public ThemeEntity findEnabledPortalTheme(final ExecutionContext executionContext) {
        try {
            return findEnvironmentPortalThemes(executionContext).filter(ThemeEntity::isEnabled).orElseGet(this::buildDefaultPortalTheme);
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find enabled theme";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public ThemeEntity findOrCreateDefaultPortalTheme(final ExecutionContext executionContext) {
        try {
            return findEnvironmentPortalThemes(executionContext).orElseGet(() -> createDefaultPortalTheme(executionContext));
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find theme or create default";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private Optional<ThemeEntity> findEnvironmentPortalThemes(final ExecutionContext executionContext) throws TechnicalException {
        return themeRepository
            .findByReferenceIdAndReferenceTypeAndType(executionContext.getEnvironmentId(), ENVIRONMENT.name(), ThemeType.PORTAL)
            .stream()
            .sorted(comparing(Theme::isEnabled, reverseOrder()))
            .findFirst()
            .map(this::convertToPortalThemeEntity);
    }

    private ThemeEntity buildDefaultPortalTheme() {
        ThemeEntity theme = new ThemeEntity();
        theme.setId(UUID.randomUUID().toString());
        theme.setName("Default theme");
        try {
            theme.setDefinition(MAPPER.readPortalDefinition(getDefaultDefinition()));
        } catch (IOException e) {
            throw new TechnicalManagementException(e);
        }
        theme.setBackgroundImage(this.getDefaultBackgroundImage());
        theme.setLogo(this.getDefaultLogo());
        theme.setOptionalLogo(this.getDefaultOptionalLogo());
        theme.setFavicon(this.getDefaultFavicon());
        return theme;
    }

    private ThemeEntity createDefaultPortalTheme(ExecutionContext executionContext) {
        return createPortalTheme(executionContext, convert(buildDefaultPortalTheme()));
    }

    /**
     * Update the default theme of type PORTAL
     * @param executionContext -- organization and environment
     */
    @Override
    public void updateDefaultPortalTheme(final ExecutionContext executionContext) {
        try {
            final Set<Theme> themes = themeRepository.findByReferenceIdAndReferenceTypeAndType(
                executionContext.getEnvironmentId(),
                ENVIRONMENT.name(),
                ThemeType.PORTAL
            );

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
                            auditService.createAuditLog(
                                executionContext,
                                Collections.singletonMap(THEME, theme.getId()),
                                THEME_UPDATED,
                                new Date(),
                                theme,
                                themeUpdate
                            );
                        } catch (IOException ex) {
                            final String error =
                                "Error while trying to merge default theme from the definition path: " +
                                themesPath +
                                DEFAULT_THEME_PATH +
                                " with theme " +
                                theme.toString();
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

    @Override
    public PictureEntity getFavicon(final ExecutionContext executionContext, String themeId) {
        try {
            final String favicon = findByIdWithoutConvert(executionContext, themeId).getFavicon();
            if (favicon != null) {
                return convertToPicture(favicon);
            }
        } catch (ThemeNotFoundException ex) {
            LOGGER.debug("Theme {} not found, using default favicon", themeId);
        }
        return convertToPicture(this.getDefaultFavicon());
    }

    public String getDefaultDefinition() {
        return this.getDefinition(themesPath + DEFAULT_THEME_PATH);
    }

    public String getDefinition(String path) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = new String(Files.readAllBytes(new File(path).toPath()), defaultCharset());
            // Important for remove formatting (space, line break...)
            JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
            return jsonNode.toString();
        } catch (IOException ex) {
            final String error = "Error while trying to load a theme from the definition path: " + path;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    public String getDefaultBackgroundImage() {
        return getImage("background-image.png");
    }

    public String getDefaultLogo() {
        return getImage("logo.png");
    }

    public String getDefaultOptionalLogo() {
        return getImage("logo-light.png");
    }

    public String getDefaultFavicon() {
        return getImage("favicon.png");
    }

    private String getImage(String filename) {
        String filepath = themesPath + "/" + filename;
        File imageFile = new File(filepath);
        if (!imageFile.exists()) {
            return null;
        }
        try {
            byte[] image = Files.readAllBytes(imageFile.toPath());
            MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
            return "data:" + fileTypeMap.getContentType(filename) + ";base64," + Base64.getEncoder().encodeToString(image);
        } catch (IOException ex) {
            final String error = "Error while trying to load image from: " + filepath;
            LOGGER.error(error, ex);
            return null;
        }
    }

    @Override
    public GenericThemeEntity resetToDefaultTheme(final ExecutionContext executionContext, String themeId) {
        try {
            LOGGER.debug("Reset to default theme by ID: {}", themeId);
            final GenericThemeEntity previousTheme = findById(executionContext, themeId);
            themeRepository.delete(previousTheme.getId());
            auditService.createAuditLog(
                executionContext,
                Collections.singletonMap(THEME, themeId),
                THEME_RESET,
                new Date(),
                previousTheme,
                null
            );
            if (io.gravitee.rest.api.model.theme.ThemeType.PORTAL.equals(previousTheme.getType())) {
                return findOrCreateDefaultPortalTheme(executionContext);
            }
            return null;
        } catch (Exception ex) {
            final String error = "Error while trying to reset a default theme";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public PictureEntity getLogo(final ExecutionContext executionContext, String themeId) {
        try {
            final String logo = findByIdWithoutConvert(executionContext, themeId).getLogo();
            if (logo != null) {
                return convertToPicture(logo);
            }
        } catch (ThemeNotFoundException ex) {
            LOGGER.debug("Theme {} not found, using default logo", themeId);
        }
        return convertToPicture(this.getDefaultLogo());
    }

    @Override
    public PictureEntity getOptionalLogo(final ExecutionContext executionContext, String themeId) {
        try {
            final String optionalLogo = findByIdWithoutConvert(executionContext, themeId).getOptionalLogo();
            if (optionalLogo != null) {
                return convertToPicture(optionalLogo);
            }
        } catch (ThemeNotFoundException ex) {
            LOGGER.debug("Theme {} not found, using default optional logo", themeId);
        }
        return convertToPicture(this.getDefaultOptionalLogo());
    }

    @Override
    public PictureEntity getBackgroundImage(final ExecutionContext executionContext, String themeId) {
        try {
            final String backgroundImage = findByIdWithoutConvert(executionContext, themeId).getBackgroundImage();
            if (backgroundImage != null) {
                return convertToPicture(backgroundImage);
            }
        } catch (ThemeNotFoundException ex) {
            LOGGER.debug("Theme {} not found, using default background image", themeId);
        }
        return convertToPicture(this.getDefaultBackgroundImage());
    }

    private PictureEntity convertToPicture(String picture) {
        if (StringUtils.isEmpty(picture)) {
            return null;
        }
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

    private Theme convert(final ExecutionContext executionContext, NewThemeEntity themeEntity) {
        try {
            final Date now = new Date();
            final Theme theme = new Theme();
            theme.setId(String.valueOf(UUID.randomUUID()));
            theme.setType(ThemeType.valueOf(themeEntity.getType().name()));
            theme.setCreatedAt(now);
            theme.setUpdatedAt(now);
            theme.setReferenceId(executionContext.getEnvironmentId());
            theme.setReferenceType(ENVIRONMENT.name());
            theme.setLogo(themeEntity.getLogo());
            theme.setName(themeEntity.getName());
            theme.setDefinition(MAPPER.writeValueAsString(themeEntity.getDefinition()));
            theme.setEnabled(themeEntity.isEnabled());
            theme.setBackgroundImage(themeEntity.getBackgroundImage());
            theme.setOptionalLogo(themeEntity.getOptionalLogo());
            theme.setFavicon(themeEntity.getFavicon());
            return theme;
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException("Cannot convert new theme entity", e);
        }
    }

    private ThemeEntity convertToPortalThemeEntity(final Theme theme) {
        final ThemeEntity themeEntity = new ThemeEntity();
        themeEntity.setId(theme.getId());
        themeEntity.setName(theme.getName());
        try {
            themeEntity.setDefinition(MAPPER.readPortalDefinition(theme.getDefinition()));
        } catch (IOException e) {
            LOGGER.error("Cannot read definition of theme " + theme.getId() + " definition:" + theme.getDefinition());
        }
        themeEntity.setCreatedAt(theme.getCreatedAt());
        themeEntity.setUpdatedAt(theme.getUpdatedAt());
        themeEntity.setEnabled(theme.isEnabled());
        themeEntity.setLogo(theme.getLogo());
        themeEntity.setBackgroundImage(theme.getBackgroundImage());
        themeEntity.setOptionalLogo(theme.getOptionalLogo());
        themeEntity.setFavicon(theme.getFavicon());
        return themeEntity;
    }

    private GenericThemeEntity convert(final Theme theme) {
        if (ThemeType.PORTAL.equals(theme.getType())) {
            return convertToPortalThemeEntity(theme);
        }

        try {
            var portalNextDefinition = MAPPER.readPortalNextDefinition(theme.getDefinition());

            return io.gravitee.rest.api.model.theme.portalnext.ThemeEntity
                .builder()
                .id(theme.getId())
                .name(theme.getName())
                .definition(portalNextDefinition)
                .createdAt(theme.getCreatedAt())
                .updatedAt(theme.getUpdatedAt())
                .logo(theme.getLogo())
                .optionalLogo(theme.getOptionalLogo())
                .favicon(theme.getFavicon())
                .build();
        } catch (IOException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private NewThemeEntity convert(ThemeEntity theme) {
        final NewThemeEntity newThemeEntity = new NewThemeEntity();
        newThemeEntity.setName(theme.getName());
        newThemeEntity.setDefinition(theme.getDefinition());
        newThemeEntity.setBackgroundImage(theme.getBackgroundImage());
        newThemeEntity.setLogo(theme.getLogo());
        newThemeEntity.setOptionalLogo(theme.getOptionalLogo());
        newThemeEntity.setFavicon(theme.getFavicon());
        return newThemeEntity;
    }

    public static class ThemeDefinitionMapper extends ObjectMapper {

        private final Logger LOGGER = LoggerFactory.getLogger(ThemeDefinitionMapper.class);

        public ThemeDefinition readPortalDefinition(String themeDefinition) throws IOException {
            return this.readValue(themeDefinition, ThemeDefinition.class);
        }

        public io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition readPortalNextDefinition(String themeDefinition)
            throws IOException {
            return this.readValue(themeDefinition, io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.class);
        }

        public ThemeDefinition merge(final String base, final String override) throws IOException {
            final ThemeDefinition overrideDefinition = this.readPortalDefinition(override);
            final ThemeDefinition overrideDefinitionFinal = this.readPortalDefinition(override);

            ThemeDefinition mergedDefinition = this.readerForUpdating(overrideDefinition).readValue(base);

            List<ThemeComponentDefinition> componentsData = mergedDefinition
                .getData()
                .stream()
                .map(component -> {
                    List<ThemeCssDefinition> cssMerged = component
                        .getCss()
                        .stream()
                        .map(css -> {
                            ThemeCssDefinition customCss = getThemeCssDefinition(
                                overrideDefinitionFinal,
                                component.getName(),
                                css.getName()
                            );
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
            return themeDefinition
                .getData()
                .stream()
                .filter(themeComponentDefinition -> name.equals(themeComponentDefinition.getName()))
                .findFirst()
                .orElse(null);
        }

        public ThemeCssDefinition getThemeCssDefinition(ThemeDefinition themeDefinition, String name, String cssName) {
            ThemeComponentDefinition componentDefinition = getThemeComponentDefinition(themeDefinition, name);
            if (componentDefinition != null) {
                return componentDefinition.getCss().stream().filter(css -> cssName.equals(css.getName())).findFirst().orElse(null);
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
