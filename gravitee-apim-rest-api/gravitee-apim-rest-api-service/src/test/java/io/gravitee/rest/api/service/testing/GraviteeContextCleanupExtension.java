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
package io.gravitee.rest.api.service.testing;

import io.gravitee.rest.api.service.common.GraviteeContext;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// Auto-registered via META-INF/services/org.junit.jupiter.api.extension.Extension
// together with junit-platform.properties enabling autodetection. Guards against
// ThreadLocal pollution leaking from one test class to the next when forks share
// a JVM (forkCount>1, reuseForks=true). Cleanup happens after the entire class
// so @BeforeAll setups inside a class keep working.
public class GraviteeContextCleanupExtension implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext context) {
        GraviteeContext.cleanContext();
    }
}
