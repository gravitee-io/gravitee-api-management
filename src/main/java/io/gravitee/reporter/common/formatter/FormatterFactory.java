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
package io.gravitee.reporter.common.formatter;

import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.common.MetricsType;
import io.gravitee.reporter.common.formatter.csv.CsvFormatter;
import io.gravitee.reporter.common.formatter.elasticsearch.ElasticsearchFormatter;
import io.gravitee.reporter.common.formatter.elasticsearch.FreeMarkerComponent;
import io.gravitee.reporter.common.formatter.json.JsonFormatter;
import io.gravitee.reporter.common.formatter.msgpack.MsgPackFormatter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FormatterFactory {

  private final Node node;

  private final FormatterFactoryConfiguration configuration;

  public FormatterFactory(
    Node node,
    FormatterFactoryConfiguration configuration
  ) {
    this.node = node;
    this.configuration = configuration;
  }

  public Formatter<Reportable> getFormatter(Type type) {
    return getFormatter(type, null);
  }

  public Formatter<Reportable> getFormatter(
    Type type,
    MetricsType metricsType
  ) {
    return switch (type) {
      case CSV -> new CsvFormatter<>();
      case MESSAGE_PACK -> new MsgPackFormatter<>(
        configuration.getRules(metricsType)
      );
      case JSON -> new JsonFormatter<>(configuration.getRules(metricsType));
      case ELASTICSEARCH -> new ElasticsearchFormatter<>(
        node,
        new FreeMarkerComponent(configuration.elasticSearchVersion)
      );
    };
  }
}
