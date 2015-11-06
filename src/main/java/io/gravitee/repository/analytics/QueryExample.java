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
package io.gravitee.repository.analytics;

import io.gravitee.repository.analytics.query.IntervalBuilder;
import io.gravitee.repository.analytics.query.Query;

import java.time.temporal.ChronoUnit;

import static io.gravitee.repository.analytics.query.QueryBuilders.query;

/**
 * Created by david on 05/11/2015.
 */
public class QueryExample {

    public static void main(String[] args) {
        Query query = query().hitsByApi("api-toto").interval(IntervalBuilder.interval(ChronoUnit.DAYS, 1)).build();
    }
}
