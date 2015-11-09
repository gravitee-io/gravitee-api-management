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

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AnalyticsTypeParam {

    public enum AnalyticsType {
        HITS,
        HITS_BY_STATUS,
        HITS_BY_LATENCY,
        HITS_BY_APIKEY
    }

    private AnalyticsType type;

    public AnalyticsTypeParam(String input) {
        try {
            if (input != null) {
                type = AnalyticsType.valueOf(input.toUpperCase());
            }
        } catch (IllegalArgumentException iae) {
            type = AnalyticsType.HITS;
        }
    }

    public AnalyticsType getType() {
        return this.type;
    }
}
