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
package io.gravitee.gateway.core.policy.impl;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Path;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.PathResolver;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A simple path resolver based on context paths definition.
 * This implementation doesn't use regex at all.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PathResolverImpl implements PathResolver {

    private final static String URL_PATH_SEPARATOR = "/";
    private final static String PATH_PARAM_PREFIX = ":";
    private final static String PATH_PARAM_REGEX = "[A-Za-z0-9_]+";

    @Autowired
    private Api api;

    private final Map<String, Pattern> regexPaths = new HashMap<>();

    @Override
    public Path resolve(Request request) {
        String path = request.path() + '/';

        for(Map.Entry<String, Path> entry : api.getPaths().entrySet()) {
            Pattern regex = regexPaths.computeIfAbsent(entry.getKey(), this::toRegexPath);
            if (regex.matcher(path).lookingAt()) {
                return entry.getValue();
            }
        }

        // Returns the root path
        // TODO: remove the getOrDefault as soon as all references to /* have been removed
        return api.getPaths().getOrDefault(URL_PATH_SEPARATOR, api.getPaths().get("/*"));
    }

    private Pattern toRegexPath(String path) {
        String [] branches = path.split(URL_PATH_SEPARATOR);
        StringBuilder buffer = new StringBuilder(URL_PATH_SEPARATOR);

        for(String branch : branches) {
            if (! branch.isEmpty()) {
                if (branch.startsWith(PATH_PARAM_PREFIX)) {
                    buffer.append(PATH_PARAM_REGEX);
                } else {
                    buffer.append(branch);
                }

                buffer.append(URL_PATH_SEPARATOR);
            }
        }

        return Pattern.compile(buffer.toString());
    }

    public void setApi(Api api) {
        this.api = api;
    }
}
