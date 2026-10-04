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
package io.gravitee.definition.model.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;

/**
 * Context policy composing native provider compaction and shared optional-control profiles.
 *
 * <p>Mode governs native provider compaction; profile composition supplies effective optional request
 * controls from the shared catalog. This does not own agent memory or durable history.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ContextManagementPolicy implements Serializable {

    public enum Mode {
        DEFAULT,
        ENFORCE,
        PASSTHROUGH,
    }

    public enum Profile {
        CUSTOM,
        ECONOMY,
        FLOW,
    }

    private final Mode mode;
    private final Profile profile;
    private final Integer compactThreshold;

    public ContextManagementPolicy(
        @JsonProperty("mode") final Mode mode,
        @JsonProperty("compactThreshold") final Integer compactThreshold,
        @JsonProperty("profile") final Profile profile
    ) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.profile = profile == null ? Profile.CUSTOM : profile;
        Integer target = this.profile != Profile.CUSTOM ? ContextManagementProfiles.compactThreshold(this.profile) : compactThreshold;
        if (mode == Mode.PASSTHROUGH && this.profile != Profile.CUSTOM) {
            throw new IllegalArgumentException("profiles are unavailable in PASSTHROUGH mode");
        }
        if (mode == Mode.PASSTHROUGH) {
            if (compactThreshold != null) {
                throw new IllegalArgumentException("compactThreshold must be absent for PASSTHROUGH mode");
            }
        } else if (target != null && target <= 0) {
            throw new IllegalArgumentException("compactThreshold must be positive for " + mode + " mode");
        }
        this.compactThreshold = target;
    }

    /** An untouched optional form group is inheritance, not a new endpoint mandate. */
    @JsonCreator
    public static ContextManagementPolicy fromJson(
        @JsonProperty("mode") Mode mode,
        @JsonProperty("compactThreshold") Integer compactThreshold,
        @JsonProperty("profile") Profile profile
    ) {
        if (mode == null && compactThreshold == null && (profile == null || profile == Profile.CUSTOM)) return null;
        return new ContextManagementPolicy(mode, compactThreshold, profile);
    }

    public Mode getMode() {
        return mode;
    }

    public Profile getProfile() {
        return profile;
    }

    public Integer getCompactThreshold() {
        return compactThreshold;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextManagementPolicy)) return false;
        ContextManagementPolicy that = (ContextManagementPolicy) o;
        return mode == that.mode && profile == that.profile && Objects.equals(compactThreshold, that.compactThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, profile, compactThreshold);
    }

    @Override
    public String toString() {
        return "ContextManagementPolicy{" + "mode=" + mode + ", profile=" + profile + ", compactThreshold=" + compactThreshold + '}';
    }
}
