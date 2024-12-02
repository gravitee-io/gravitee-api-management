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
import io.gravitee.reporter.common.formatter.Formatter;
import io.vertx.rxjava3.core.buffer.Buffer;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class BulkFormatterTransformer implements BulkTransformer {

  private final Formatter<Reportable> formatter;

  @Override
  public TransformedReport transform(final Reportable reportable) {
    return new TransformedReport(
      Buffer.newInstance(
        formatter.format(reportable, buildOptions(reportable))
      ),
      reportable.getClass()
    );
  }

  protected Map<String, Object> buildOptions(final Reportable reportable) {
    return null;
  }
}
