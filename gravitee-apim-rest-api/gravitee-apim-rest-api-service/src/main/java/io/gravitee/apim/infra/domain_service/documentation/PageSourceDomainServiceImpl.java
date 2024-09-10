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
package io.gravitee.apim.infra.domain_service.documentation;

import io.gravitee.apim.core.documentation.domain_service.PageSourceDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageSourceException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.fetcher.api.Fetcher;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@AllArgsConstructor
public class PageSourceDomainServiceImpl implements PageSourceDomainService {

    private final FetcherConfigurationFactory fetcherConfigurationFactory;
    private final PluginManager<FetcherPlugin<?>> pluginManager;
    private final ApplicationContext applicationContext;

    @Override
    public void setContentFromSource(Page page) {
        loadFetcher(page).ifPresentOrElse(fetcher -> fetchContent(fetcher, page), () -> page.setUseAutoFetch(false));
    }

    private void fetchContent(Fetcher fetcher, Page page) {
        if (page.getType() != Page.Type.ROOT) {
            page.setContent(readContent(fetcher, page.getSource()));
        }
        page.setUseAutoFetch(fetcher.getConfiguration().isAutoFetch());
    }

    private String readContent(Fetcher fetcher, PageSource source) {
        try {
            var resource = fetcher.fetch();
            var content = resource.getContent();
            return new String(content.readAllBytes(), Charset.defaultCharset());
        } catch (FetcherException | IOException e) {
            throw new TechnicalDomainException("unable to fetch content with configuration " + source.getConfiguration());
        }
    }

    private Optional<Fetcher> loadFetcher(Page page) {
        return Optional.ofNullable(page.getSource()).flatMap(this::loadPlugin).map(plugin -> buildFetcher(plugin, page.getSource()));
    }

    private Optional<FetcherPlugin<?>> loadPlugin(PageSource source) {
        return Optional.ofNullable(pluginManager.get(source.getType()));
    }

    @SuppressWarnings("unchecked")
    private Fetcher buildFetcher(FetcherPlugin<?> plugin, PageSource source) {
        try {
            var classLoader = plugin.fetcher().getClassLoader();
            var configClass = (Class<? extends FetcherConfiguration>) classLoader.loadClass(plugin.configuration().getName());
            var config = fetcherConfigurationFactory.create(configClass, source.getConfiguration());
            var fetcherClass = (Class<? extends Fetcher>) classLoader.loadClass(plugin.clazz());
            var fetcher = fetcherClass.getConstructor(configClass).newInstance(config);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(fetcher);
            return fetcher;
        } catch (Exception e) {
            throw new TechnicalDomainException("unable to build fetcher instance", e);
        }
    }
}
