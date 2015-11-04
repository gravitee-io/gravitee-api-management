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

import io.gravitee.repository.analytics.model.FilterQuery;
import io.gravitee.repository.analytics.model.FilterQueryType;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class FilterQueryBuilder {

    public static FilterQuery api(final String apiName) {
        return new FilterQuery() {
            @Override
            public FilterQueryType type() {
                return FilterQueryType.API_NAME;
            }

            @Override
            public String value() {
                return apiName;
            }
        };
    }

    public static FilterQuery apiKey(final String apiKey) {
        return new FilterQuery() {
            @Override
            public FilterQueryType type() {
                return FilterQueryType.API_KEY;
            }

            @Override
            public String value() {
                return apiKey;
            }
        };
    }
}
