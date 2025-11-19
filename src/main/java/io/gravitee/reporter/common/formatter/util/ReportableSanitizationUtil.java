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

package io.gravitee.reporter.common.formatter.util;

import io.gravitee.reporter.api.v4.common.Message;
import java.util.Map;

public final class ReportableSanitizationUtil {

  private ReportableSanitizationUtil() {
    // util class
  }

  public static void removeCustomMetricsWithNullValues(
    Map<String, String> customMetrics
  ) {
    if (customMetrics != null) {
      customMetrics.entrySet().removeIf(entry -> entry.getValue() == null);
    }
  }

  public static void removeMessageMetadataWithNullValues(Message message) {
    if (message != null && message.getMetadata() != null) {
      message
        .getMetadata()
        .entrySet()
        .removeIf(entry -> entry.getValue() == null);
    }
  }
}
