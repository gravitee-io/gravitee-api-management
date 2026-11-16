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
package io.gravitee.apim.reporter.elasticsearch.config;

import io.gravitee.elasticsearch.utils.Type;

/**
 * The types sharing one lifecycle setting: {@code lifecycle.policies.<key>} and {@code lifecycle.retention.<key>}. The v4 types
 * have no keys of their own and follow their v2 counterpart.
 */
public enum LifecycleGroup {
    REQUEST("request"),
    LOG("log"),
    MONITOR("monitor"),
    HEALTH("health"),
    EVENT_METRICS("event_metrics"),
    DECISIONS("decisions");

    public static final String POLICY_SETTING_PREFIX = "reporters.elasticsearch.lifecycle.policies.";
    public static final String RETENTION_SETTING_PREFIX = "reporters.elasticsearch.lifecycle.retention.";

    private final String key;

    LifecycleGroup(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public String policySetting() {
        return POLICY_SETTING_PREFIX + key;
    }

    public String retentionSetting() {
        return RETENTION_SETTING_PREFIX + key;
    }

    public static LifecycleGroup of(Type type) {
        return switch (type) {
            case REQUEST, V4_METRICS, V4_MESSAGE_METRICS -> REQUEST;
            case LOG, V4_LOG, V4_MESSAGE_LOG -> LOG;
            case MONITOR -> MONITOR;
            case HEALTH_CHECK -> HEALTH;
            case EVENT_METRICS -> EVENT_METRICS;
            case DECISIONS -> DECISIONS;
        };
    }
}
