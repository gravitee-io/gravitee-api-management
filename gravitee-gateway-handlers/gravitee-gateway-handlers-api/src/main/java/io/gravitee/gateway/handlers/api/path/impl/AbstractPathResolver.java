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
package io.gravitee.gateway.handlers.api.path.impl;

import io.gravitee.gateway.handlers.api.path.Path;
import io.gravitee.gateway.handlers.api.path.PathResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A simple path resolver based on context paths definition.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPathResolver implements PathResolver {

    private final static String URL_PATH_SEPARATOR = "/";
    private final static String PATH_PARAM_PREFIX = ":";
    private final static String PATH_PARAM_REGEX = "[a-z0-9\\-._~%!$&'()*+,;=:@/]+";

    private final List<Path> registeredPaths = new ArrayList<>();

    private final String contextPath;

    protected AbstractPathResolver(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public Path resolve(final String path) {
        String tmpPath = path + '/';

        int pieces = -1;
        Path bestPath = null;

        for(Path registerPath : registeredPaths) {
            if (registerPath.getPattern().matcher(tmpPath).lookingAt()) {
                int split = registerPath.getPath().split(URL_PATH_SEPARATOR).length;
                if (split > pieces) {
                    pieces = split;
                    bestPath = registerPath;
                }
            }
        }

        return bestPath;
    }

    protected void register(Path path) {
        String [] branches = path.getResolvedPath().split(URL_PATH_SEPARATOR);
        StringBuilder buffer = new StringBuilder(contextPath);

        if (buffer.charAt(buffer.length() - 1) != '/') {
            buffer.append(URL_PATH_SEPARATOR);
        }

        path.setPath(buffer.substring(0, buffer.length() - 1) + path.getResolvedPath());

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


        path.setPattern(Pattern.compile(buffer.toString()));
        registeredPaths.add(path);
    }
}
