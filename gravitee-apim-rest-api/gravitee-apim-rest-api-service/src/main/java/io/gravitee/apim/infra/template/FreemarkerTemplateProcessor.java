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
package io.gravitee.apim.infra.template;

import freemarker.core.TemplateClassResolver;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

/**
 * Implementation of {@link TemplateProcessor} using Freemarker.
 */
@Service
public class FreemarkerTemplateProcessor implements TemplateProcessor {

    @Override
    public String processInlineTemplate(String template, Map<String, Object> params) throws TemplateProcessorException {
        final freemarker.template.Configuration configuration = new freemarker.template.Configuration(
            freemarker.template.Configuration.VERSION_2_3_22
        );
        configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);

        try {
            Template freemarkerTemplate = new Template("", new StringReader(template), configuration);
            return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerTemplate, params);
        } catch (TemplateException | IOException e) {
            throw new TemplateProcessorException(e);
        }
    }
}
