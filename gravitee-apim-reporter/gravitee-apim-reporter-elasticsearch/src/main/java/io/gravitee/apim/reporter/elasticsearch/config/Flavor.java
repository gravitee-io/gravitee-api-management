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

import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import java.util.Locale;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * The kind of cluster the reporter writes to: a classic Elasticsearch or OpenSearch cluster, or Elastic Cloud Serverless, which
 * answers with a regular version number but manages indices itself.
 */
public enum Flavor {
    CLASSIC,
    SERVERLESS;

    public static final String SETTING = "reporters.elasticsearch.flavor";
    private static final String AUTO = "auto";
    private static final String SERVERLESS_BUILD_FLAVOR = "serverless";

    /**
     * @return the flavor forced by the setting, or empty when it is left to detection
     * @throws IllegalArgumentException when the setting holds an unknown value
     */
    public static Optional<Flavor> configured(String setting) {
        if (!StringUtils.hasText(setting) || AUTO.equalsIgnoreCase(setting.trim())) {
            return Optional.empty();
        }
        for (Flavor flavor : values()) {
            if (flavor.value().equalsIgnoreCase(setting.trim())) {
                return Optional.of(flavor);
            }
        }
        throw new IllegalArgumentException(
            "Unknown value [" + setting + "] for '" + SETTING + "': expected one of auto, classic, serverless."
        );
    }

    public static Flavor detected(ElasticsearchInfo info) {
        return info.getBuildFlavor().filter(SERVERLESS_BUILD_FLAVOR::equals).isPresent() ? SERVERLESS : CLASSIC;
    }

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
