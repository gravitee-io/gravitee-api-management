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
package io.gravitee.reporter.common.bulk.sender;

import io.gravitee.common.service.Service;
import io.gravitee.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.reporter.common.bulk.transformer.TransformedReport;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface BulkSender extends Service<BulkSender> {
  /**
   * Called as a last step of the bulk process to send a bulk of reportable represented by a {@link CompressedBulk} to the target reporter
   * </p>
   * The returned Completable can signal an error using a {@link io.gravitee.reporter.common.bulk.exception.NonRetryableException}
   * to indicate a failure that should not be retried. Any other exception will be treated as retryable.
   *
   * @param bulk
   * @return {@link Completable} of the result
   */
  Completable send(final CompressedBulk bulk);
}
