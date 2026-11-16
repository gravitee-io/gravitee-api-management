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
package io.gravitee.repository.elasticsearch.configuration;

import java.util.Locale;

/**
 * How analytics indices are named: one index per day, an ILM-managed alias, or a data stream. The last two are read as
 * {@code <prefix>-<type>}.
 */
public enum IndexMode {
    DAILY,
    ILM,
    DATA_STREAM;

    /**
     * @return the mode the setting names, daily for any value it does not know
     */
    public static IndexMode of(String setting) {
        for (IndexMode mode : values()) {
            if (mode.value().equalsIgnoreCase(setting.trim())) {
                return mode;
            }
        }
        return DAILY;
    }

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
