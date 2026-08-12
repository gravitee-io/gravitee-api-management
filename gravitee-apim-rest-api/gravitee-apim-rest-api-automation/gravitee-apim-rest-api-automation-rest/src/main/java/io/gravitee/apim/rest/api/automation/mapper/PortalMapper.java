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

import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.PortalNavigationPath;
import io.gravitee.apim.rest.api.automation.model.PortalNavigationStructure;
import io.gravitee.apim.rest.api.automation.model.PortalSpec;
import io.gravitee.apim.rest.api.automation.model.PortalState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper
public interface PortalMapper {
    PortalMapper INSTANCE = Mappers.getMapper(PortalMapper.class);

    default PortalState toPortalState(
        Portal portal,
        String hrid,
        io.gravitee.apim.core.portal.model.PortalNavigationStructure structure,
        List<Validator.Error> errors
    ) {
        var state = new PortalState(
            portal.getId() != null ? portal.getId().toString() : null,
            portal.getEnvironmentId(),
            portal.getOrganizationId(),
            toErrors(errors)
        );
        state.setHrid(hrid);
        mapPortalToState(portal, state);
        var wireStructure = new PortalNavigationStructure();
        structure.areas().forEach((area, paths) -> writeWireArea(wireStructure, area, toApiNavigation(paths)));
        state.setStructure(wireStructure);
        state.setNavigation(wireStructure.getTopNavbar());
        return state;
    }

    private static void writeWireArea(PortalNavigationStructure wireStructure, PortalArea area, List<PortalNavigationPath> paths) {
        switch (area) {
            case TOP_NAVBAR -> wireStructure.setTopNavbar(paths);
            case HOMEPAGE -> navigationNotAllowedFor(area);
        }
    }

    private static void navigationNotAllowedFor(PortalArea area) {
        throw new IllegalArgumentException("Setting navigation for " + area + " area is not allowed.");
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "hrid", ignore = true)
    @Mapping(target = "navigation", ignore = true)
    @Mapping(target = "structure", ignore = true)
    void mapPortalToState(Portal portal, @MappingTarget PortalState state);

    default io.gravitee.apim.core.portal.model.PortalNavigationStructure toCoreStructure(PortalSpec spec) {
        Map<PortalArea, List<NavigationPath>> areas = new EnumMap<>(PortalArea.class);
        var topNavbar = resolveTopNavbar(spec);
        if (!topNavbar.isEmpty()) {
            areas.put(PortalArea.TOP_NAVBAR, topNavbar);
        }
        return new io.gravitee.apim.core.portal.model.PortalNavigationStructure(areas);
    }

    private static List<NavigationPath> resolveTopNavbar(PortalSpec spec) {
        if (spec.getStructure() != null) {
            return toCoreNavigationPathList(spec.getStructure().getTopNavbar());
        }
        return toCoreNavigationPathList(spec.getNavigation());
    }

    private static List<NavigationPath> toCoreNavigationPathList(List<PortalNavigationPath> navigation) {
        if (navigation == null) {
            return List.of();
        }
        List<NavigationPath> list = new ArrayList<>();
        for (int i = 0; i < navigation.size(); i++) {
            PortalNavigationPath n = navigation.get(i);
            if (isValidNavigationPath(n)) {
                list.add(new NavigationPath(n.getPath(), n.getDisplayName(), i));
            }
        }
        return list;
    }

    default List<PortalNavigationPath> toApiNavigation(List<io.gravitee.apim.core.portal.model.NavigationPath> navigation) {
        if (navigation == null) {
            return List.of();
        }
        return navigation
            .stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(np -> np.order() == null ? 0 : np.order()))
            .map(n -> new PortalNavigationPath().path(n.path()).displayName(n.displayName()))
            .toList();
    }

    private static boolean isValidNavigationPath(PortalNavigationPath n) {
        return n != null && !n.getPath().isBlank();
    }
}
