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

import static io.gravitee.reporter.common.formatter.Mappers.JSON;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.configuration.Rules;
import io.gravitee.reporter.common.MetricsType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class AbstractFormatterTest {

  private static final String BASE_PATH =
    "io/gravitee/reporter/common/formatter";

  private static final String BASE_API_PACKAGE = "io.gravitee.reporter.api.";

  private static final Node NODE = when(mock(Node.class).id())
    .thenReturn("gateway-id")
    .getMock();

  private static final FormatterFactory FACTORY = new FormatterFactory(
    NODE,
    FormatterFactoryConfiguration
      .builder()
      .rule(
        MetricsType.REQUEST_LOG,
        rules(
          Set.of("*"),
          Set.of("clientRequest.uri", "clientResponse.status", "timestamp"),
          Map.of("timestamp", "date")
        )
      )
      .rule(
        MetricsType.REQUEST,
        rules(
          Set.of("*"),
          Set.of("timestamp", "api", "proxyLatencyMs"),
          Map.of("timestamp", "date")
        )
      )
      .rule(
        MetricsType.NODE_MONITOR,
        rules(
          Set.of("*"),
          Set.of("timestamp", "os"),
          Map.of("timestamp", "date")
        )
      )
      .rule(
        MetricsType.HEALTH_CHECK,
        rules(
          Set.of("*"),
          Set.of("timestamp", "api", "success"),
          Map.of("timestamp", "date")
        )
      )
      .build()
  );

  protected Formatter<Reportable> formatter;

  protected abstract Type type();

  @BeforeEach
  void setUp() {
    resetFormatter();
  }

  protected void resetFormatter(MetricsType metricsType) {
    formatter = FACTORY.getFormatter(type(), metricsType);
  }

  protected void resetFormatter() {
    formatter = FACTORY.getFormatter(type());
  }

  protected Reportable readGiven(String path, String className) {
    try {
      var fullyQualifiedClassName = BASE_API_PACKAGE + className;
      var clazz = Class
        .forName(fullyQualifiedClassName)
        .asSubclass(Reportable.class);
      return readGiven(path, clazz);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to read class", e);
    }
  }

  protected <T extends Reportable> T readGiven(String path, Class<T> clazz) {
    var filePath = BASE_PATH + "/given/" + path;
    try {
      return JSON.readValue(getResourceAsStream(filePath), clazz);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read given resource", e);
    }
  }

  protected String readExpected(String path) {
    var filePath = BASE_PATH + "/expected/" + path;
    try {
      return new String(getResourceAsStream(filePath).readAllBytes());
    } catch (IOException e) {
      throw new IllegalStateException(
        "Unable to read expected bytes from file " + filePath,
        e
      );
    }
  }

  private InputStream getResourceAsStream(String filePath) {
    var resourceAsStream = getClass()
      .getClassLoader()
      .getResourceAsStream(filePath);

    if (resourceAsStream == null) {
      throw new IllegalArgumentException(
        "Unable to find expected file " + filePath
      );
    }

    return resourceAsStream;
  }

  private static Rules rules(
    Set<String> excluded,
    Set<String> included,
    Map<String, String> renamed
  ) {
    var rules = new Rules();
    rules.setExcludeFields(excluded);
    rules.setIncludeFields(included);
    rules.setRenameFields(renamed);
    return rules;
  }
}
