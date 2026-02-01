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
// TODO: REVERT - Build fix for plan/subscription merge. Added getAttributes(), 4-arg constructor, removed @Builder.
// Revert once merge conflicts are properly resolved upstream.
package fakes;

import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.License;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class FakeLicense implements License {

    private String referenceType;
    private String referenceId;
    private String tier;
    private final Set<String> packs = new HashSet<>();
    private final Set<String> features = new HashSet<>();
    private Date expirationDate;
    private final Map<String, Object> attributes = new HashMap<>();

    public FakeLicense(String referenceType, String referenceId, String tier, Date expirationDate) {
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.tier = tier;
        this.expirationDate = expirationDate;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean isFeatureEnabled(String feature) {
        return features.contains(feature);
    }

    @Override
    public void verify() throws InvalidLicenseException {}

    @Override
    public boolean isExpired() {
        return expirationDate.before(new Date());
    }

    @NotNull
    @Override
    public Map<String, String> getRawAttributes() {
        return attributes
            .entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey(), e.getValue().toString()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
