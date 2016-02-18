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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.redis.junit.RedisExternalResource;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        RedisApiKeyRepositoryTest.class,
        RedisApiRepositoryTest.class,
        RedisApplicationRepositoryTest.class,
        RedisEventRepositoryTest.class
})
public class RedisManagementSuiteTest {


    @ClassRule
    public static RedisExternalResource redisExternalResource = new RedisExternalResource();
    
}
