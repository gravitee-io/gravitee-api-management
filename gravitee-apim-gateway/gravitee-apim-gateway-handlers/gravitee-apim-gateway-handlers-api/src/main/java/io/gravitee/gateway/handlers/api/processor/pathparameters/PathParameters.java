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
package io.gravitee.gateway.handlers.api.processor.pathparameters;

import io.gravitee.definition.model.flow.Operator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathParameters {

    private static final String PATH_SEPARATOR = "/";
    private static final Pattern SEPARATOR_SPLITTER = Pattern.compile(PATH_SEPARATOR);
    private static final String PATH_PARAM_PREFIX = ":";
    private static final String PATH_PARAM_REGEX = "[a-zA-Z0-9\\-._~%!$&'()* +,;=:@|]+";

    private final String originalPath;
    private final Operator operator;
    private Pattern pathPattern;
    private final List<PathParameter> parameters = new ArrayList<PathParameter>();

    public PathParameters(String originalPath, Operator operator) {
        this.originalPath = originalPath;
        this.operator = operator;
        extractPathParamsAndPattern();
    }

    private void extractPathParamsAndPattern() {
        String[] branches = SEPARATOR_SPLITTER.split(originalPath);
        StringBuilder patternizedPath = new StringBuilder(PATH_SEPARATOR);

        for (int i = 0; i < branches.length; i++) {
            if (!branches[i].isEmpty()) {
                if (branches[i].startsWith(PATH_PARAM_PREFIX)) {
                    String paramWithoutColon = branches[i].substring(1);
                    PathParameter pathParameter = new PathParameter(paramWithoutColon, parameters.size());
                    parameters.add(pathParameter);
                    patternizedPath.append("(?<" + pathParameter.getId() + ">" + PATH_PARAM_REGEX + ")");
                } else {
                    patternizedPath.append(branches[i]);
                }

                // Do not add a trailing slash for last branch
                if (i < branches.length - 1) {
                    patternizedPath.append(PATH_SEPARATOR);
                }
            }
        }

        pathPattern = operator.equals(Operator.STARTS_WITH)
            ? Pattern.compile("^" + patternizedPath + "(?:/.*)?$")
            : Pattern.compile("^" + patternizedPath + "/?$");
    }

    public Pattern getPathPattern() {
        return pathPattern;
    }

    public List<PathParameter> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathParameters that = (PathParameters) o;
        return Objects.equals(originalPath, that.originalPath) && Objects.equals(operator, that.operator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalPath, operator);
    }
}
