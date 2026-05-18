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
package io.gravitee.apim.core.template;

import java.util.Map;

/**
 * Interface of component handling Templates.
 */
public interface TemplateProcessor {
    /**
     * Render an inline template with no specific name. Equivalent to
     * {@link #processInlineTemplate(String, String, Map)} with an empty name.
     */
    String processInlineTemplate(String template, Map<String, Object> params) throws TemplateProcessorException;

    /**
     * Render an inline template. The {@code name} is surfaced in error messages and engine diagnostics
     * (typically the id of the entity owning the template, e.g. a portal page id).
     */
    default String processInlineTemplate(String name, String template, Map<String, Object> params) throws TemplateProcessorException {
        return processInlineTemplate(template, params);
    }
}
