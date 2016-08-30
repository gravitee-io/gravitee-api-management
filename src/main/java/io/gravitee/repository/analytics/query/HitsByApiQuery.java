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
package io.gravitee.repository.analytics.query;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HitsByApiQuery extends TimeRangedQuery {

    private String api;
    private Type type = Type.HITS;

    void api(String api) {
        this.api = api;
    }

    public String api() {
        return this.api;
    }

    void type(Type type) {
        this.type = type;
    }

    public Type type() {
        return this.type;
    }

    public enum Type {
        HITS,
        HITS_BY_LATENCY,
        HITS_BY_APIKEY,
        HITS_BY_STATUS,
        HITS_BY_PAYLOAD_SIZE,
        HITS_BY_APPLICATION,
        TOP_HITS_BY_APPLICATION,
        TOP_HITS_BY_STATUS
    }
}
