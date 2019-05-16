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
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class FreeMarkerComponent {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(FreeMarkerComponent.class);

    /**
     * The name of the directory containing the freemarker templates.
     */
    private static final String DIRECTORY_NAME = "/freemarker";

    /** Freemarker configuration */
    private Configuration configuration;

    /**
     * Initialize FreeMarker.
     */
    {
        this.configuration = new Configuration(Configuration.VERSION_2_3_23);
        this.configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        this.configuration.setDateFormat("iso_utc");
        this.configuration.setLocale(Locale.ENGLISH);
        this.configuration.setNumberFormat("computer");
        this.configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
        this.configuration.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), DIRECTORY_NAME);
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
            logger.error("Impossible to generate from template {}", templateName, exception);
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
