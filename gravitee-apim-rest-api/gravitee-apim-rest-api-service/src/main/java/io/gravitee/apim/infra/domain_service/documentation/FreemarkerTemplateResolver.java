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
package io.gravitee.apim.infra.domain_service.documentation;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.gravitee.apim.core.documentation.domain_service.TemplateResolverDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

public class FreemarkerTemplateResolver implements TemplateResolverDomainService {

    @Override
    public String resolveTemplate(String content, Map<String, Object> params) throws InvalidPageContentException {
        Configuration freemarkerConfiguration = initFreemarkerConfiguration();
        try {
            Template template = new Template("template", new StringReader(content), freemarkerConfiguration);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
        } catch (IOException | TemplateException e) {
            throw new InvalidPageContentException(e);
        }
    }

    private Configuration initFreemarkerConfiguration() {
        // Init the configuration
        final freemarker.template.Configuration configuration = new freemarker.template.Configuration(
            freemarker.template.Configuration.VERSION_2_3_22
        );

        configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);

        configuration.setTemplateLoader(new StringTemplateLoader());
        return configuration;
    }
}
