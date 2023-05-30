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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static fixtures.GraviteeLicenseFixtures.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class GraviteeLicenseMapperTest {

    private final GraviteeLicenseMapper licenseMapper = Mappers.getMapper(GraviteeLicenseMapper.class);

    @Test
    void shouldConvertToLicense() {
        var license = licenseMapper.convert(aGraviteeLicenseEntity());
        assertThat(license).usingRecursiveComparison().isEqualTo(aGraviteeLicense());
    }
}
