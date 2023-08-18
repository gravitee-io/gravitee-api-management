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
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.buffer.Buffer;
import java.util.Map;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractFormatter<T extends Reportable>
  implements Formatter<T> {

  private static final String REMOTE_ADDRESS_FALLBACK = "0.0.0.0";

  @Override
  public Buffer format(T reportable) {
    return format(reportable, null);
  }

  @Override
  public Buffer format(T reportable, Map<String, Object> options) {
    if (reportable instanceof Metrics metrics) {
      metrics.setRemoteAddress(
        sanitizeRemoteAddress(metrics.getRemoteAddress())
      );
    }

    if (
      reportable instanceof io.gravitee.reporter.api.v4.metric.Metrics metrics
    ) {
      metrics.setRemoteAddress(
        sanitizeRemoteAddress(metrics.getRemoteAddress())
      );
    }

    return options == null ? format0(reportable) : format0(reportable, options);
  }

  private static String sanitizeRemoteAddress(String remoteAddress) {
    return InetAddressValidator.getInstance().isValid(remoteAddress)
      ? remoteAddress
      : REMOTE_ADDRESS_FALLBACK;
  }

  protected abstract Buffer format0(T data);

  @SuppressWarnings("unused")
  protected Buffer format0(T data, Map<String, Object> options) {
    return format0(data);
  }
}
