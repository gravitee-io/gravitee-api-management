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
package io.gravitee.rest.api.service.impl;

import io.gravitee.fetcher.api.Fetcher;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.Resource;

public class PageService_MockSensitiveFetcher implements Fetcher {

    private final PageService_MockSensitiveFetcherConfiguration configuration;

    public PageService_MockSensitiveFetcher(PageService_MockSensitiveFetcherConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Resource fetch() {
        throw new UnsupportedOperationException("not fetched by these tests");
    }

    @Override
    public FetcherConfiguration getConfiguration() {
        return configuration;
    }
}
