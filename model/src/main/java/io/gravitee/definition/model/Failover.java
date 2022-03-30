/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Failover implements Serializable {

    public static int DEFAULT_MAX_ATTEMPTS = 1;
    public static long DEFAULT_RETRY_TIMEOUT = 10000L;
    public static FailoverCase[] DEFAULT_FAILOVER_CASES = { FailoverCase.TIMEOUT };

    @JsonProperty("maxAttempts")
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    @JsonProperty("retryTimeout")
    private long retryTimeout = DEFAULT_RETRY_TIMEOUT;

    @JsonProperty("cases")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private FailoverCase[] cases = DEFAULT_FAILOVER_CASES;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public FailoverCase[] getCases() {
        return cases;
    }

    public void setCases(FailoverCase[] cases) {
        this.cases = cases;
    }

    public long getRetryTimeout() {
        return retryTimeout;
    }

    public void setRetryTimeout(long retryTimeout) {
        this.retryTimeout = retryTimeout;
    }
}
