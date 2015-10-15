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
package io.gravitee.repository.mongodb.ratelimit;

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimitResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class RateLimitRepositoryImpl implements RateLimitRepository<String> {

    @Override
    public RateLimitResult acquire(String key, int pound, long limit, long periodTime, TimeUnit periodTimeUnit) {
        return null;
    }
}
