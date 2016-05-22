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
package io.gravitee.management.rest.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Provider
@PreMatching
public class CorsResponseFilter implements ContainerResponseFilter {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(CorsResponseFilter.class);

    /**
     * Add the cross domain data to the output if needed.
     *
     * @param reqCtx The container request (input)
     * @param respCtx The container request (output)
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        LOGGER.debug("Executing CORS response filter");

        respCtx.getHeaders().add("Access-Control-Allow-Origin", reqCtx.getHeaderString("origin"));
        respCtx.getHeaders().addAll("Access-Control-Allow-Headers", "Cache-Control", "Pragma", "Origin", "Authorization", "Content-Type", "X-Requested-With");
        respCtx.getHeaders().add("Access-Control-Allow-Credentials", "true");
        respCtx.getHeaders().addAll("Access-Control-Allow-Methods", "GET", "POST", "DELETE", "PUT", "OPTIONS", "X-XSRF-TOKEN");
        respCtx.getHeaders().add("Access-Control-Max-Age", "1209600");
    }
}
