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
package io.gravitee.reporter.common.bulk.backpressure;

import io.gravitee.reporter.common.bulk.compressor.CompressedBulk;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class BulkDropper {

  private final Map<String, Integer> countPerType = new ConcurrentHashMap<>(8);

  public enum Reason {
    ERROR,
    OVERFLOW,
  }

  public void drop(CompressedBulk bulk, Reason reason) {
    bulk
      .countPerType()
      .forEach((bulkType, count) ->
        countPerType.compute(
          bulkType,
          (counterType, total) -> total == null ? count : total + count
        )
      );

    if (reason == Reason.OVERFLOW) {
      log.warn(
        "Overflow detected. Dropping bulk of reports to avoid too much memory pressure [{}]. Total: [{}].",
        bulk,
        countPerType
      );
    } else {
      log.warn(
        "Bulk error detected. Dropping bulk of reports [{}]. Total: [{}].",
        bulk,
        countPerType
      );
    }
  }
}
