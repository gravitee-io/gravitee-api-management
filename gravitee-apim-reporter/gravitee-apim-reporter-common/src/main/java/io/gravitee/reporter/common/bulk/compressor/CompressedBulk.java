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
package io.gravitee.reporter.common.bulk.compressor;

import io.vertx.rxjava3.core.buffer.Buffer;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a bulk of reports that have been compressed into a single buffer.
 * It also allows to hold a counter per type of report that have been compressed.
 *
 * @param compressed the compressed buffer.
 * @param countPerType a {@link Map} that contains the number of reports compressed per type.
 */
public record CompressedBulk(
  Buffer compressed,
  Map<String, Integer> countPerType
) {
  @Override
  public String toString() {
    return Objects.toString(countPerType);
  }
}
