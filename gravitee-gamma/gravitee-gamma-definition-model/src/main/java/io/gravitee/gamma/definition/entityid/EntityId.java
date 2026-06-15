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
package io.gravitee.gamma.definition.entityid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.regex.Pattern;

/**
 * A parsed {@code entityId} (ADR-037): the string {@code <type>.<slug>} used by authz, lineage and the UI. The one
 * parsing rule for everyone is "split on the first dot": the type is the first segment, the slug is everything after
 * it (so a scoped slug keeps its parent, e.g. {@code mcp-tool} + {@code github.list_pull_request}).
 *
 * <p>The environment is a separate dimension and never part of the string; a cross-environment reference is the pair
 * {@code (environment, entityId)}.
 */
public record EntityId(String type, String slug) {
    /** A single slug segment: lowercase, digits, underscore, dash. */
    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9_-]+");
    /** A dotted path of slug segments (the slug of a scoped id keeps its parent). */
    private static final Pattern SLUG_PATH = Pattern.compile("[a-z0-9_-]+(?:\\.[a-z0-9_-]+)*");
    /** The full {@code <type>.<slug>} string: at least two segments. */
    private static final Pattern FORMAT = Pattern.compile("[a-z0-9_-]+(?:\\.[a-z0-9_-]+)+");

    public EntityId {
        if (type == null || !SEGMENT.matcher(type).matches()) {
            throw new IllegalArgumentException("type must be a slug segment: " + type);
        }
        if (slug == null || !SLUG_PATH.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug must be a dotted path of slug segments: " + slug);
        }
    }

    /** Whether {@code value} is a well-formed {@code <type>.<slug>} string. */
    public static boolean isValid(String value) {
        return value != null && FORMAT.matcher(value).matches();
    }

    /** The full {@code <type>.<slug>} string. This is the JSON form: a single self-contained token, never a pair. */
    @JsonValue
    public String value() {
        return type + "." + slug;
    }

    /**
     * The last segment of the slug. A consumer that already holds the parent context (e.g. the PEP of a specific
     * MCP proxy) can match on this alone instead of the full hierarchical id.
     */
    public String lastSegment() {
        int dot = slug.lastIndexOf('.');
        return dot < 0 ? slug : slug.substring(dot + 1);
    }

    /** Parse {@code <type>.<slug>} by splitting on the first dot. Also the JSON deserializer (from the string form). */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EntityId parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("entityId must not be null");
        }
        int dot = value.indexOf('.');
        if (dot <= 0 || dot == value.length() - 1) {
            throw new IllegalArgumentException("entityId must be '<type>.<slug>': " + value);
        }
        return new EntityId(value.substring(0, dot), value.substring(dot + 1));
    }

    /** Build a top-level id, slugging the name. */
    public static EntityId of(String type, String name) {
        return new EntityId(type, requireSlug(name));
    }

    /** Build a scoped id {@code <type>.<parentSlug>.<slug>}; the parent slug is taken as-is (already a slug). */
    public static EntityId scoped(String type, String parentSlug, String name) {
        if (parentSlug == null || parentSlug.isBlank()) {
            throw new IllegalArgumentException("parentSlug must not be blank");
        }
        return new EntityId(type, parentSlug + "." + requireSlug(name));
    }

    private static String requireSlug(String name) {
        String slug = EntitySlug.toSlug(name);
        if (slug == null) {
            throw new IllegalArgumentException("name does not produce a slug: " + name);
        }
        return slug;
    }
}
