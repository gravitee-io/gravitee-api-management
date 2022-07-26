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
package io.gravitee.rest.api.service.v4.impl.validation;

import io.gravitee.definition.model.Cors;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.service.exceptions.AllowOriginNotAllowedException;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CorsValidationServiceImplTest {

    private CorsValidationService corsValidationService;

    @Before
    public void setUp() throws Exception {
        corsValidationService = new CorsValidationServiceImpl();
    }

    @Test(expected = AllowOriginNotAllowedException.class)
    public void shouldHaveAllowOriginNotAllowed() throws TechnicalException {
        Cors cors = new Cors();
        cors.setAccessControlAllowOrigin(
            Sets.newSet(
                "http://example.com",
                "localhost",
                "https://10.140.238.25:8080",
                "(http|https)://[a-z]{6}.domain.[a-zA-Z]{2,6}",
                ".*.company.com",
                "/test^"
            )
        );
        corsValidationService.validateAndSanitize(cors);
    }

    @Test
    public void shouldHaveAllowOriginWildcardAllowed() throws TechnicalException {
        Cors cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Collections.singleton("*"));
        Cors sanitizedCors = corsValidationService.validateAndSanitize(cors);

        Assert.assertSame(cors, sanitizedCors);
    }

    @Test
    public void shouldHaveAllowOriginNullAllowed() throws TechnicalException {
        Cors cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Collections.singleton("null"));
        Cors sanitizedCors = corsValidationService.validateAndSanitize(cors);

        Assert.assertSame(cors, sanitizedCors);
    }
}
