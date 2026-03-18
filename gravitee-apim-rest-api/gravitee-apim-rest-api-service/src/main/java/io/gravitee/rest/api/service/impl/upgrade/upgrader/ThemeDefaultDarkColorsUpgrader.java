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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.THEME_DEFAULT_DARK_COLORS_UPGRADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.management.model.ThemeType;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Background;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.DarkMode;
import java.util.Date;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Adds default dark mode colors to existing PORTAL_NEXT themes that were created before dark mode support.
 * Themes with null or empty dark.color get the default dark palette from Key.java.
 */
@Component
@CustomLog
public class ThemeDefaultDarkColorsUpgrader implements Upgrader {

    private static final DarkMode DEFAULT_DARK_MODE = DarkMode.builder()
        .color(
            Color.builder()
                .primary("#8BABF8")
                .secondary("#6A95D4")
                .tertiary("#8BABF8")
                .error("#F2B8B5")
                .background(Background.builder().page("#1C1B1F").card("#2B2930").build())
                .build()
        )
        .build();

    private final EnvironmentRepository environmentRepository;
    private final ThemeRepository themeRepository;

    public ThemeDefaultDarkColorsUpgrader(@Lazy EnvironmentRepository environmentRepository, @Lazy ThemeRepository themeRepository) {
        this.environmentRepository = environmentRepository;
        this.themeRepository = themeRepository;
    }

    @Override
    public int getOrder() {
        return THEME_DEFAULT_DARK_COLORS_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        try {
            int upgraded = 0;
            for (var environment : environmentRepository.findAll()) {
                Set<Theme> themes = themeRepository.findByReferenceIdAndReferenceTypeAndType(
                    environment.getId(),
                    "ENVIRONMENT",
                    ThemeType.PORTAL_NEXT
                );
                for (Theme theme : themes) {
                    if (needsUpgrade(theme)) {
                        themeRepository.update(withDefaultDarkColors(theme));
                        upgraded++;
                    }
                }
            }
            if (upgraded > 0) {
                log.info("ThemeDefaultDarkColorsUpgrader: added default dark colors to {} theme(s)", upgraded);
            }
            return true;
        } catch (TechnicalException e) {
            log.error("Failed to apply ThemeDefaultDarkColorsUpgrader", e);
            return false;
        }
    }

    private boolean needsUpgrade(Theme theme) {
        if (theme.getDefinition() == null || theme.getDefinition().isBlank()) return false;
        try {
            ThemeDefinition def = GraviteeJacksonMapper.getInstance().readValue(theme.getDefinition(), ThemeDefinition.class);
            return def != null && (def.getDark() == null || def.getDark().getColor() == null);
        } catch (Exception e) {
            log.warn("Theme [{}]: could not parse definition, skipping", theme.getId(), e);
            return false;
        }
    }

    private Theme withDefaultDarkColors(Theme theme) throws TechnicalException {
        try {
            ThemeDefinition def = GraviteeJacksonMapper.getInstance().readValue(theme.getDefinition(), ThemeDefinition.class);
            def.setDark(DEFAULT_DARK_MODE);
            theme.setDefinition(GraviteeJacksonMapper.getInstance().writeValueAsString(def));
            theme.setUpdatedAt(new Date());
            return theme;
        } catch (JsonProcessingException e) {
            throw new TechnicalException("Failed to serialize theme definition", e);
        }
    }
}
