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
package io.gravitee.reporter.common.bulk.transformer;

import io.gravitee.reporter.api.Reportable;
import io.vertx.rxjava3.core.buffer.Buffer;
import java.util.Locale;

/**
 * Represents a report (log, metrics, request, ...) that has been transformed into its final format ('json', 'csv', 'elasticsearch', ...).
 *
 * @param transformed the report that has been transformed.
 * @param clazz the initial type of the report.
 */
public record TransformedReport(
  Buffer transformed,
  Class<? extends Reportable> clazz
) {
  public String type() {
    return clazz.getSimpleName().toLowerCase(Locale.ROOT);
  }
}
