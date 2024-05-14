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
package fixtures.core.model;

import fakes.FakeLicense;
import io.gravitee.node.api.license.License;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class LicenseFixtures {

    public static License anEnterpriseLicense() {
        return new FakeLicense(
            License.REFERENCE_TYPE_PLATFORM,
            License.REFERENCE_ID_PLATFORM,
            "universe",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS))
        );
    }

    public static License anOssLicense() {
        return new FakeLicense(
            License.REFERENCE_TYPE_PLATFORM,
            License.REFERENCE_ID_PLATFORM,
            "oss",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS))
        );
    }
}
