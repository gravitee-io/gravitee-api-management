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
package io.gravitee.apim.rest.api.automation.mapper;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.PortalNextThemeBackground;
import io.gravitee.apim.rest.api.automation.model.PortalNextThemeColor;
import io.gravitee.apim.rest.api.automation.model.PortalNextThemeDefinition;
import io.gravitee.apim.rest.api.automation.model.PortalNextThemeFont;
import io.gravitee.apim.rest.api.automation.model.PortalThemeState;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalThemeMapper {
    PortalThemeMapper INSTANCE = Mappers.getMapper(PortalThemeMapper.class);

    default PortalThemeState toPortalThemeState(Theme theme, String hrid, AuditInfo audit, List<Validator.Error> errors) {
        var state = new PortalThemeState(theme.getId(), audit.environmentId(), audit.organizationId(), toErrors(errors));
        state.setHrid(hrid);
        state.setName(theme.getName());
        state.setDefinition(toWireDefinition(theme.getDefinitionPortalNext()));
        state.setLogo(theme.getLogo());
        state.setOptionalLogo(theme.getOptionalLogo());
        state.setFavicon(theme.getFavicon());
        state.setBackgroundImage(theme.getBackgroundImage());
        return state;
    }

    default ThemeDefinition toCoreDefinition(PortalNextThemeDefinition wire) {
        if (wire == null) {
            return null;
        }
        return ThemeDefinition.builder()
            .customCss(wire.getCustomCss())
            .font(toCoreFont(wire.getFont()))
            .color(toCoreColor(wire.getColor()))
            .build();
    }

    default PortalNextThemeDefinition toWireDefinition(ThemeDefinition core) {
        if (core == null) {
            return null;
        }
        return new PortalNextThemeDefinition()
            .customCss(core.getCustomCss())
            .font(toWireFont(core.getFont()))
            .color(toWireColor(core.getColor()));
    }

    default Errors toErrors(List<Validator.Error> validationErrors) {
        if (validationErrors == null || validationErrors.isEmpty()) {
            return null;
        }
        var wire = new Errors();
        wire.setSevere(validationErrors.stream().filter(Validator.Error::isSevere).map(Validator.Error::getMessage).toList());
        wire.setWarning(validationErrors.stream().filter(Validator.Error::isWarning).map(Validator.Error::getMessage).toList());
        return wire;
    }

    private static ThemeDefinition.Font toCoreFont(PortalNextThemeFont wire) {
        if (wire == null) {
            return null;
        }
        return ThemeDefinition.Font.builder().fontFamily(wire.getFontFamily()).build();
    }

    private static PortalNextThemeFont toWireFont(ThemeDefinition.Font core) {
        if (core == null) {
            return null;
        }
        return new PortalNextThemeFont().fontFamily(core.getFontFamily());
    }

    private static ThemeDefinition.Color toCoreColor(PortalNextThemeColor wire) {
        if (wire == null) {
            return null;
        }
        return ThemeDefinition.Color.builder()
            .primary(wire.getPrimary())
            .secondary(wire.getSecondary())
            .tertiary(wire.getTertiary())
            .error(wire.getError())
            .background(toCoreBackground(wire.getBackground()))
            .build();
    }

    private static PortalNextThemeColor toWireColor(ThemeDefinition.Color core) {
        if (core == null) {
            return null;
        }
        return new PortalNextThemeColor()
            .primary(core.getPrimary())
            .secondary(core.getSecondary())
            .tertiary(core.getTertiary())
            .error(core.getError())
            .background(toWireBackground(core.getBackground()));
    }

    private static ThemeDefinition.Background toCoreBackground(PortalNextThemeBackground wire) {
        if (wire == null) {
            return null;
        }
        return ThemeDefinition.Background.builder().page(wire.getPage()).card(wire.getCard()).build();
    }

    private static PortalNextThemeBackground toWireBackground(ThemeDefinition.Background core) {
        if (core == null) {
            return null;
        }
        return new PortalNextThemeBackground().page(core.getPage()).card(core.getCard());
    }
}
