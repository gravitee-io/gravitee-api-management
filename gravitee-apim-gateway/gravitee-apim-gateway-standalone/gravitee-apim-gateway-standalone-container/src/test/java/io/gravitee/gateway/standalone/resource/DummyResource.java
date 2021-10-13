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
package io.gravitee.gateway.standalone.resource;

import io.gravitee.resource.api.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyResource extends AbstractResource {

    private final Logger logger = LoggerFactory.getLogger(DummyResource.class);

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        logger.info("Starting resource {}", name());
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        logger.info("Stopping resource {}", name());
    }

    @Override
    public String name() {
        return "Dummy Resource";
    }

    public String getResourceName() {
        return name();
    }
}
