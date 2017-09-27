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
package io.gravitee.gateway.el;

import io.gravitee.gateway.api.expression.TemplateEngine;

/**
 * @deprecated replaced by io.gravitee.el.spel.SpelTemplateEngine
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Deprecated
public class SpelTemplateEngine implements TemplateEngine {

    private final io.gravitee.el.spel.SpelTemplateEngine delegate = new io.gravitee.el.spel.SpelTemplateEngine();

    @Override
    public String convert(String expression) {
        return delegate.convert(expression);
    }

    @Override
    public SpelTemplateContext getTemplateContext() {
        return new SpelTemplateContext((io.gravitee.el.spel.SpelTemplateContext) delegate.getTemplateContext());
    }
}
