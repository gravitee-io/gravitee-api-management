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
package io.gravitee.gateway.flow;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import java.util.*;
import java.util.regex.Pattern;

public abstract class AbstractFlowParametersProvider implements FlowProvider {

    private static final String URL_PATH_SEPARATOR = "/";
    private static final Pattern URL_PATH_SEPARATOR_PATTERN = Pattern.compile(URL_PATH_SEPARATOR);
    private static final String PATH_PARAM_PREFIX = ":";

    public void addContextRequestPathParameters(ExecutionContext context, Flow flow) {
        if (flow.getPath() == null || flow.getPath().isEmpty()) {
            return;
        }
        Map<Integer, String> parameters = new HashMap();
        String[] branches = URL_PATH_SEPARATOR_PATTERN.split(flow.getPath());

        for (int position = 0; position < branches.length; position++) {
            final String branch = branches[position];
            if (branch.startsWith(PATH_PARAM_PREFIX)) {
                parameters.put(position, branch.substring(PATH_PARAM_PREFIX.length()));
            }
        }

        if (parameters.isEmpty()) {
            return;
        }

        int count = 0;
        int off = 0;
        int next;

        final String pathInfo = context.request().pathInfo();
        final Iterator<Integer> iterator = parameters.keySet().iterator();

        Integer currentPosition = iterator.next();
        while ((next = pathInfo.indexOf(URL_PATH_SEPARATOR, off)) != -1) {
            if (count == currentPosition) {
                context.request().pathParameters().add(parameters.get(currentPosition), pathInfo.substring(off, next));
                if (iterator.hasNext()) {
                    currentPosition = iterator.next();
                }
            }
            off = next + 1;
            count++;
        }

        context.request().pathParameters().add(parameters.get(currentPosition), pathInfo.substring(off));
    }
}
