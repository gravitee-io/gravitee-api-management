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
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.gravitee.apim.core.documentation.domain_service.TemplateResolverDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import io.gravitee.apim.infra.template.FreemarkerConfigurationFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

@Service
public class FreemarkerTemplateResolver implements TemplateResolverDomainService {

    private final Configuration configuration;

    public FreemarkerTemplateResolver() {
        this.configuration = initFreemarkerConfiguration();
    }

    @Override
    public String resolveTemplate(String content, Map<String, Object> params) throws InvalidPageContentException {
        try {
            Template template = new Template("template", new StringReader(content), configuration);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
        } catch (IOException e) {
            throw new InvalidPageContentException("Invalid template " + e.getMessage(), e);
        } catch (TemplateException e) {
            throw new InvalidPageContentException("Invalid expression " + e.getBlamedExpressionString(), e);
        }
    }

    private Configuration initFreemarkerConfiguration() {
        // Init the configuration
        final Configuration configuration = FreemarkerConfigurationFactory.createSecureConfiguration();

        configuration.setTemplateLoader(new StringTemplateLoader());
        return configuration;
    }
}
