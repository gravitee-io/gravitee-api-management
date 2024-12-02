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
package io.gravitee.reporter.common.bulk;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public record BulkConfiguration(
  Integer items,
  Long flushInterval,
  Integer maxConcurrentSend,
  Integer maxRetries,
  Integer retryInitialDelay,
  Integer retryMaxDelay,
  Long maxMemorySize
) {
  public static final int DEFAULT_ITEMS = 1000;
  public static final long DEFAULT_FLUSH_INTERVAL = 5L;
  public static final int DEFAULT_MAX_CONCURRENT_SEND = 100;
  public static final Integer DEFAULT_RETRY_MAX_RETRIES = 6;
  public static final int DEFAULT_RETRY_INITIAL_DELAY = 3000;
  public static final int DEFAULT_RETRY_MAX_DELAY = 30000;
  public static final long DEFAULT_MAX_MEMORY_SIZE = 26214400;

  public BulkConfiguration {
    if (items == null) {
      items = DEFAULT_ITEMS;
    }
    if (flushInterval == null) {
      flushInterval = DEFAULT_FLUSH_INTERVAL;
    }
    if (maxConcurrentSend == null) {
      maxConcurrentSend = DEFAULT_MAX_CONCURRENT_SEND;
    }
    if (maxRetries == null) {
      maxRetries = DEFAULT_RETRY_MAX_RETRIES;
    }
    if (retryInitialDelay == null) {
      retryInitialDelay = DEFAULT_RETRY_INITIAL_DELAY;
    }
    if (retryMaxDelay == null) {
      retryMaxDelay = DEFAULT_RETRY_MAX_DELAY;
    }
    if (maxMemorySize == null) {
      maxMemorySize = DEFAULT_MAX_MEMORY_SIZE;
    }

    if (maxMemorySize.equals(DEFAULT_MAX_MEMORY_SIZE)) {
      log.warn(
        "You are using the default 'maxMemorySize' ({}MB). Consider increase this value in case of too frequent 'Dropping bulk of reports' events.",
        (DEFAULT_MAX_MEMORY_SIZE / 1024 / 1024)
      );
    }
  }
}
