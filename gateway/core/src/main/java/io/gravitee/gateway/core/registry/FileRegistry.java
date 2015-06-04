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
package io.gravitee.gateway.core.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.model.Api;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * File API registry.
 * This registry is based on JSON processed files to provide Gateway configuration.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class FileRegistry extends AbstractRegistry {

    private final static String JSON_EXTENSION = ".json";

    public FileRegistry() {
        this(System.getProperty("gateway.conf", "/etc/gravitee.io/conf"));
    }

    public FileRegistry(String configurationPath) {
        File configuration = new File(configurationPath);

        if (configuration.exists()) {
            readConfiguration(configuration);
        } else {
            LOGGER.error("No configuration can be read from {}",
                    configuration.getAbsolutePath());
        }
    }

    private void readConfiguration(File configuration) {
        LOGGER.info("Loading Gravitee configuration from {}", configuration.getAbsolutePath());

        Set<File> configurations = lookingForConfigurationFiles(configuration);

        // Initialize Jackson mapper to read json files
        ObjectMapper mapper = new ObjectMapper();

        // Read all configuration files
        for(File conf : configurations) {
            try {
                register(mapper.readValue(conf, Api.class));
            } catch (IOException ioe) {
                LOGGER.error("Unable to read file : {}", conf, ioe);
            }
        }

        LOGGER.info("{} API(s) registered", listAll().size());
    }

    private Set<File> lookingForConfigurationFiles(File configuration) {
        if (configuration.isFile()) {
            LOGGER.debug("Provided configuration path is a file...");

            // Check if provided file is suffixed with .json
            if (! configuration.getName().endsWith(JSON_EXTENSION)) {
                LOGGER.error("Configuration file is not a JSON file (does not end with .json)");
                throw new IllegalStateException("Configuration file is not a JSON file (does not end with .json)");
            }

            return Collections.singleton(configuration);
        } else {
            LOGGER.debug("Provided configuration is a directory, looking for json files.");
            File [] confs = configuration.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(JSON_EXTENSION);
                }
            });

            return new HashSet<File>(Arrays.asList(confs));
        }
    }
}
