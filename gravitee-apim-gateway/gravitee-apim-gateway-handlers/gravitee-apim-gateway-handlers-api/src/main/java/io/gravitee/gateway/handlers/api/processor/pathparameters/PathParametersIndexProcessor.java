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
package io.gravitee.gateway.handlers.api.processor.pathparameters;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.gateway.handlers.api.path.Path;
import io.gravitee.gateway.handlers.api.path.PathParam;
import io.gravitee.gateway.handlers.api.path.PathResolver;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyResolver;
import java.util.Iterator;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathParametersIndexProcessor extends AbstractProcessor<ExecutionContext> {

    private static final char URL_PATH_SEPARATOR = '/';

    private PathResolver pathResolver;

    public PathParametersIndexProcessor(PathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    @Override
    public void handle(ExecutionContext context) {
        Path path = getResolvedPath(context);
        final List<PathParam> parameters = path.getParameters();

        if (parameters != null && !parameters.isEmpty()) {
            int count = 0;
            int off = 0;
            int next;

            final String pathInfo = context.request().pathInfo();
            final Iterator<PathParam> iterator = parameters.iterator();

            PathParam currentParameter = iterator.next();
            while ((next = pathInfo.indexOf(URL_PATH_SEPARATOR, off)) != -1) {
                if (count == currentParameter.getPosition()) {
                    context.request().pathParameters().add(currentParameter.getName(), pathInfo.substring(off, next));
                    if (iterator.hasNext()) {
                        currentParameter = iterator.next();
                    }
                }
                off = next + 1;
                count++;
            }
            context.request().pathParameters().add(currentParameter.getName(), pathInfo.substring(off));
        }

        next.handle(context);
    }

    private Path getResolvedPath(ExecutionContext context) {
        // Resolve the "configured" path according to the inbound request
        Path path = pathResolver.resolve(context.request());

        context.setAttribute(ApiPolicyResolver.API_RESOLVED_PATH, path);

        // Used, at least, by rate-limit / quota policies
        context.setAttribute(ExecutionContext.ATTR_RESOLVED_PATH, path.getPath());
        return path;
    }
}
