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

import io.gravitee.reporter.api.Reportable;
import io.vertx.core.buffer.Buffer;
import java.util.Map;

public interface Formatter<T extends Reportable> {
  Buffer format(T reportable);

  @SuppressWarnings("unused")
  default Buffer format(T reportable, Map<String, Object> options) {
    return format(reportable);
  }
}
