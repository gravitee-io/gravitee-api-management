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
package io.gravitee.management.rest.resource.param;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AnalyticsParam {

    @QueryParam("from")
    private long from;

    @QueryParam("to")
    private long to;

    @QueryParam("interval")
    private long interval;

    @QueryParam("type")
    @DefaultValue("HITS")
    private AnalyticsTypeParam type;

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public AnalyticsTypeParam getTypeParam() {
        return type;
    }

    public void setTypeParam(AnalyticsTypeParam type) {
        this.type = type;
    }
}
