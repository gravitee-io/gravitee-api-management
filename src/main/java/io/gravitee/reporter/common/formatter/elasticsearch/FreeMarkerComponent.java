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
package io.gravitee.reporter.common.formatter.elasticsearch;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FreeMarkerComponent {

  /** Logger. */
  private final Logger logger = LoggerFactory.getLogger(
    FreeMarkerComponent.class
  );

  /**
   * The name of the directory containing the freemarker templates.
   */
  private static final String DIRECTORY_NAME_PATTERN = "/freemarker/es%dx";

  /** Freemarker configuration */
  private final Configuration configuration;

  public FreeMarkerComponent(int elasticsearchVersion) {
    configuration = new Configuration(Configuration.VERSION_2_3_23);
    configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
    configuration.setDateFormat("iso_utc");
    configuration.setLocale(Locale.ENGLISH);
    configuration.setNumberFormat("computer");
    configuration.setNewBuiltinClassResolver(
      TemplateClassResolver.SAFER_RESOLVER
    );
    configuration.setTemplateLoader(
      new ClassTemplateLoader(
        FreeMarkerComponent.class.getClassLoader(),
        String.format(DIRECTORY_NAME_PATTERN, elasticsearchVersion)
      )
    );
  }

  /**
   * Generate a string from a FreeMarker template.
   * @param templateName name of the FreeMarker template
   * @param data data of the template
   */
  void generateFromTemplate(
    final String templateName,
    final Map<String, Object> data,
    Writer writer
  ) {
    try {
      final Template template = this.configuration.getTemplate(templateName);
      template.process(data, writer);
    } catch (final IOException | TemplateException exception) {
      logger.error(
        "Impossible to generate from template " + templateName,
        exception
      );
      throw new IllegalArgumentException();
    }
  }
}
