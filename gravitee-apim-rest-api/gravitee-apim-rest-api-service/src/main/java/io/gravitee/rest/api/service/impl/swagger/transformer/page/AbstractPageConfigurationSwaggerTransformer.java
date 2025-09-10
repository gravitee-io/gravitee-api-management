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
package io.gravitee.rest.api.service.impl.swagger.transformer.page;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.service.impl.swagger.transformer.SwaggerTransformer;
import io.gravitee.rest.api.service.swagger.SwaggerDescriptor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class AbstractPageConfigurationSwaggerTransformer<T extends SwaggerDescriptor> implements SwaggerTransformer<T> {

    protected final PageEntity page;

    protected AbstractPageConfigurationSwaggerTransformer(PageEntity page) {
        this.page = page;
    }

    protected String getProperty(String property) {
        if (page.getConfiguration() == null) {
            return null;
        }

        return page.getConfiguration().get(property);
    }

    protected boolean asBoolean(String property) {
        String value = getProperty(property);
        return (value != null) && Boolean.parseBoolean(value);
    }

    protected String asString(String property) {
        return getProperty(property);
    }
}
