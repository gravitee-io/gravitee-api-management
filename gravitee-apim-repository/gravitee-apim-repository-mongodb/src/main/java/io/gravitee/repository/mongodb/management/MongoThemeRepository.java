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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.mongodb.management.internal.ThemeMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ThemeMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoThemeRepository implements ThemeRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoThemeRepository.class);

    @Autowired
    private ThemeMongoRepository internalThemeRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Theme> findById(String themeId) throws TechnicalException {
        LOGGER.debug("Find theme by ID [{}]", themeId);

        final ThemeMongo theme = internalThemeRepo.findById(themeId).orElse(null);

        LOGGER.debug("Find theme by ID [{}] - Done", themeId);
        return Optional.ofNullable(mapper.map(theme, Theme.class));
    }

    @Override
    public Theme create(Theme theme) throws TechnicalException {
        LOGGER.debug("Create theme [{}]", theme.getName());

        ThemeMongo themeMongo = mapper.map(theme, ThemeMongo.class);
        ThemeMongo createdThemeMongo = internalThemeRepo.insert(themeMongo);

        Theme res = mapper.map(createdThemeMongo, Theme.class);

        LOGGER.debug("Create theme [{}] - Done", theme.getName());

        return res;
    }

    @Override
    public Theme update(Theme theme) throws TechnicalException {
        if (theme == null || theme.getName() == null) {
            throw new IllegalStateException("Theme to update must have a name");
        }

        final ThemeMongo themeMongo = internalThemeRepo.findById(theme.getId()).orElse(null);

        if (themeMongo == null) {
            throw new IllegalStateException(String.format("No theme found with name [%s]", theme.getId()));
        }

        try {
            //Update
            themeMongo.setName(theme.getName());
            themeMongo.setDefinition(theme.getDefinition());
            themeMongo.setEnabled(theme.isEnabled());
            themeMongo.setReferenceId(theme.getReferenceId());
            themeMongo.setReferenceType(theme.getReferenceType());
            themeMongo.setCreatedAt(theme.getCreatedAt());
            themeMongo.setUpdatedAt(theme.getUpdatedAt());
            themeMongo.setLogo(theme.getLogo());
            themeMongo.setOptionalLogo(theme.getOptionalLogo());
            themeMongo.setBackgroundImage(theme.getBackgroundImage());
            themeMongo.setFavicon(theme.getFavicon());

            ThemeMongo themeMongoUpdated = internalThemeRepo.save(themeMongo);
            return mapper.map(themeMongoUpdated, Theme.class);
        } catch (Exception e) {
            LOGGER.error("An error occured when updating theme", e);
            throw new TechnicalException("An error occured when updating theme");
        }
    }

    @Override
    public void delete(String themeId) throws TechnicalException {
        try {
            internalThemeRepo.deleteById(themeId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting theme [{}]", themeId, e);
            throw new TechnicalException("An error occured when deleting theme");
        }
    }

    @Override
    public Set<Theme> findAll() {
        final List<ThemeMongo> themes = internalThemeRepo.findAll();
        return themes.stream().map(themeMongo -> mapper.map(themeMongo, Theme.class)).collect(Collectors.toSet());
    }

    @Override
    public Set<Theme> findByReferenceIdAndReferenceType(String referenceId, String referenceType) {
        final Set<ThemeMongo> themes = internalThemeRepo.findByReferenceIdAndReferenceType(referenceId, referenceType);
        return themes.stream().map(themeMongo -> mapper.map(themeMongo, Theme.class)).collect(Collectors.toSet());
    }
}
