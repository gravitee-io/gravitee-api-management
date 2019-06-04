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
package io.gravitee.rest.api.management.fetcher;

import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.rest.api.management.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.management.fetcher.impl.FetcherConfigurationFactoryImpl;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author Nicolas GERAUD (nicolas.geraud [at] graviteesource [dot] com) 
 * @author GraviteeSource Team
 */
public class FetcherConfigurationFactoryTest {
    private FetcherConfigurationFactory fetcherConfigurationFactory;

    @Before
    public void setUp() {
        fetcherConfigurationFactory = new FetcherConfigurationFactoryImpl();
    }

    @Test
    public void createFetcherWithConfigurationAndWithoutConfigurationData() {
        FetcherConfiguration fetcherConfiguration = fetcherConfigurationFactory.create(DummyFetcherConfiguration.class, null);

        Assert.assertNull(fetcherConfiguration);
    }

    @Test
    public void createFetcherWithConfigurationAndEmptyConfigurationData() {
        FetcherConfiguration fetcherConfiguration = fetcherConfigurationFactory.create(DummyFetcherConfiguration.class, "");

        Assert.assertNull(fetcherConfiguration);
    }

    @Test
    public void createFetcherWithConfigurationAndConfigurationData01() {
        try (InputStream is = FetcherConfigurationFactoryTest.class.getResourceAsStream("fetcher-configuration-01.json")) {
            String configuration = IOUtils.toString(is, "UTF-8");
            DummyFetcherConfiguration fetcherConfiguration = fetcherConfigurationFactory.create(DummyFetcherConfiguration.class, configuration);

            Assert.assertNotNull(fetcherConfiguration);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void createFetcherWithConfigurationAndConfigurationData02() {
        try (InputStream is = FetcherConfigurationFactoryTest.class.getResourceAsStream("fetcher-configuration-02.json")) {
            String configuration = IOUtils.toString(is, "UTF-8");
            DummyFetcherConfiguration fetcherConfiguration = fetcherConfigurationFactory.create(DummyFetcherConfiguration.class, configuration);

            Assert.assertNull(fetcherConfiguration);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
