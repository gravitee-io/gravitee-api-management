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
package io.gravitee.reporter.file.config;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.reporter.api.configuration.Rules;
import io.gravitee.reporter.file.MetricsType;
import io.gravitee.reporter.file.formatter.Type;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileReporterConfiguration {

    private static final String FILE_REPORTER_PREFIX = "reporters.file.";

    /**
     *  Reporter file name.
     */
    @Value("${reporters.file.fileName:#{systemProperties['gravitee.home']}/metrics/%s-yyyy_mm_dd}")
    private String filename;

    @Value("${reporters.file.output:json}")
    private String outputType;

    @Value("${reporters.file.flushInterval:1000}")
    private long flushInterval;

    @Value("${reporters.file.retainDays:0}")
    private long retainDays;

    @Autowired
    private ConfigurableEnvironment environment;

    public String getFilename() {
        return filename;
    }

    public Type getOutputType() {
        return outputType == null ? Type.JSON : Type.valueOf(outputType.toUpperCase());
    }

    public long getFlushInterval() {
        return flushInterval;
    }

    public long getRetainDays() {
        return retainDays;
    }

    public Rules getRules(MetricsType type) {
        Rules rules = new Rules();

        rules.setRenameFields(getMapProperties(FILE_REPORTER_PREFIX + type.getType() + ".rename"));
        rules.setExcludeFields(getArrayProperties(FILE_REPORTER_PREFIX + type.getType() + ".exclude"));
        rules.setIncludeFields(getArrayProperties(FILE_REPORTER_PREFIX + type.getType() + ".include"));

        return rules;
    }

    private Map<String, String> getMapProperties(String prefix) {
        Map<String, Object> properties = EnvironmentUtils.getPropertiesStartingWith(environment, prefix);
        if (!properties.isEmpty()) {
            return properties
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        entry -> entry.getKey().substring(EnvironmentUtils.encodedKey(prefix).length() + 1),
                        entry -> entry.getValue().toString()
                    )
                );
        } else {
            return Collections.emptyMap();
        }
    }

    private Set<String> getArrayProperties(String prefix) {
        final Set<String> properties = new HashSet<>();

        boolean found = true;
        int idx = 0;

        while (found) {
            String property = environment.getProperty(prefix + '[' + idx++ + ']');
            found = (property != null && !property.isEmpty());

            if (found) {
                properties.add(property);
            }
        }

        return properties;
    }
}
