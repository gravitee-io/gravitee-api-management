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
package io.gravitee.apim.rest.api.mcp.core.model;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A message to the people APIM knows around an API, resolved from its memberships and subscriptions.
 */
public record Notification(Audience audience, Channel channel, String subject, String text, String webhookUrl) {
    /** Who, in the terms of APIM's membership model. */
    public enum Audience {
        /** The primary owner of each application subscribed to the API. */
        CONSUMER_OWNERS(List.of("PRIMARY_OWNER")),
        /** Every member of each application subscribed to the API. */
        CONSUMER_MEMBERS(List.of("PRIMARY_OWNER", "OWNER", "USER")),
        /** The users who made the subscriptions. */
        SUBSCRIBERS(List.of("API_SUBSCRIBERS")),
        /** A webhook URL; no people. */
        WEBHOOK(List.of());

        private final List<String> applicationRoles;

        Audience(List<String> applicationRoles) {
            this.applicationRoles = applicationRoles;
        }

        /** The application roles APIM resolves this audience from, as its messaging service names them. */
        public List<String> applicationRoles() {
            return applicationRoles;
        }

        public static Audience parse(String value) {
            return Notification.parse(Audience.class, value, "audience");
        }
    }

    public enum Channel {
        PORTAL,
        MAIL,
        WEBHOOK;

        public static Channel parse(String value) {
            return Notification.parse(Channel.class, value, "channel");
        }
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String what) {
        if (value != null) {
            for (var candidate : type.getEnumConstants()) {
                if (candidate.name().equalsIgnoreCase(value.trim())) {
                    return candidate;
                }
            }
        }
        var accepted = Arrays.stream(type.getEnumConstants())
            .map(c -> c.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(", "));
        throw new TriageException("Invalid " + what + " '" + value + "'. Use one of: " + accepted + ".");
    }
}
