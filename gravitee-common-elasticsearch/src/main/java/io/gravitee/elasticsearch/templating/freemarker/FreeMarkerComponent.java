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
package io.gravitee.elasticsearch.templating.freemarker;

import freemarker.core.TemplateClassResolver;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * Utility Spring bean that encapsulates FreeMarker tools.
 * 
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 */
public class FreeMarkerComponent implements InitializingBean {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(FreeMarkerComponent.class);

    /**
     * The name of the directory containing the freemarker templates.
     */
    private static final String DIRECTORY_NAME = "/freemarker";

    /** Freemarker configuration */
    private Configuration configuration;
    @Value("${reporters.elasticsearch.template_mapping.path:#{null}}")
    private String templatesPath;

    /**
     * Initialize FreeMarker.
     */
    public void afterPropertiesSet() throws IOException {
        configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setDateFormat("iso_utc");
        configuration.setLocale(Locale.ENGLISH);
        configuration.setNumberFormat("computer");
        configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);

        final ClassTemplateLoader ctl = new ClassTemplateLoader(FreeMarkerComponent.class.getClassLoader(), DIRECTORY_NAME);
        if (templatesPath == null) {
            configuration.setTemplateLoader(ctl);
        } else {
            final FileTemplateLoader ftl = new FileTemplateLoader(new File(URLDecoder.decode(templatesPath, StandardCharsets.UTF_8.name())));
            configuration.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{ftl, ctl}));
        }
    }

    /**
     * Generate a string from a FreeMarker template.
     * @param templateName name of the FreeMarker template
     * @param data data of the template
     * @return the string generated from the template
     */
    public String generateFromTemplate(final String templateName, final Map<String, Object> data) {
        try (final StringWriter output = new StringWriter()) {
            generateFromTemplate(templateName, data, output);
            return output.getBuffer().toString();
        } catch (final IOException exception) {
            logger.error("Impossible to generate from template {}", templateName, exception);
            throw new IllegalArgumentException();
        }
    }

    /**
     * Generate a string from a FreeMarker template.
     * @param templateName name of the FreeMarker template
     * @param data data of the template
     * @return the string generated from the template
     */
    public void generateFromTemplate(final String templateName, final Map<String, Object> data, Writer writer) {
        try {
            final Template template = this.configuration.getTemplate(templateName);
            template.process(data, writer);
        } catch (final IOException | TemplateException exception) {
            logger.error("Impossible to generate from template " + templateName, exception);
            throw new IllegalArgumentException();
        }
    }

    /**
     * Generate a string from a FreeMarker template.
     * @param templateName name of the FreeMarker template
     * @return the string generated from the template
     */
    public String generateFromTemplate(final String templateName) {
        return this.generateFromTemplate(templateName, Collections.emptyMap());
    }
}
