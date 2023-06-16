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
package io.gravitee.rest.api.service.v4.impl;

import static java.util.stream.Collectors.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.Feature;
import io.gravitee.node.api.license.License;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.license.GraviteeLicenseEntity;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.v4.GraviteeLicenseService;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GraviteeLicenseServiceImpl implements GraviteeLicenseService {

    private static final String TIER_KEY = "tier";
    private static final String PACKS_KEY = "packs";
    private static final String FEATURE_VALUE = "included";
    private static final String SEPARATOR = ",";

    private final Node node;

    public GraviteeLicenseServiceImpl(Node node) {
        this.node = node;
    }

    @Override
    public GraviteeLicenseEntity getLicense() {
        var licenseEntity = new GraviteeLicenseEntity();
        licenseEntity.setTier(readTier());
        licenseEntity.setPacks(readPacks());
        licenseEntity.setFeatures(readFeatures());
        return licenseEntity;
    }

    @Override
    public boolean isFeatureEnabled(String featureName) {
        return findLicense().map(license -> extractFeatures(license).contains(featureName)).orElse(false);
    }

    private String readTier() {
        return readString(TIER_KEY).orElse(null);
    }

    private Set<String> readPacks() {
        return readString(PACKS_KEY).map(packs -> Set.of(packs.split(SEPARATOR))).orElse(Set.of());
    }

    private Set<String> readFeatures() {
        return findLicense().map(this::extractFeatures).orElse(Set.of());
    }

    private Set<String> extractFeatures(@NotNull License license) {
        return license
            .features()
            .entrySet()
            .stream()
            .filter(entry -> FEATURE_VALUE.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(toSet());
    }

    private Optional<String> readString(String featureKey) {
        return findLicense().flatMap(license -> license.feature(featureKey)).map(Feature::getString);
    }

    private Optional<License> findLicense() {
        return Optional.ofNullable(node.license());
    }
}
