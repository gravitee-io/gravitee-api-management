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
package io.gravitee.rest.api.service.impl;

import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.FilepathAwareFetcherConfiguration;

/**
 * @author GraviteeSource Team
 */
public class PageService_MockAutoFetchDescriptorFetcherConfiguration implements FetcherConfiguration, FilepathAwareFetcherConfiguration {

    private static String cron = "* * * * * *";

    @Override
    public String getFilepath() {
        return "/.gravitee.json";
    }

    @Override
    public void setFilepath(String filepath) {}

    @Override
    public boolean isAutoFetch() {
        return true;
    }

    @Override
    public String getFetchCron() {
        return cron;
    }

    public static void forceCronValue(String value) {
        cron = value;
    }
}
