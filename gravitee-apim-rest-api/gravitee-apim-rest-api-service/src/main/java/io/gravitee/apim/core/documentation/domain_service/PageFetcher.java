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
package io.gravitee.apim.core.documentation.domain_service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.fetcher.api.Fetcher;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.fetcher.api.FilepathAwareFetcherConfiguration;
import io.gravitee.fetcher.api.FilesFetcher;
import io.gravitee.fetcher.api.Resource;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class PageFetcher {

    private ImportConfiguration importConfiguration;
    private PluginManager<FetcherPlugin> fetcherPluginManager;
    private FetcherConfigurationFactory fetcherConfigurationFactory;
    private ApplicationContext applicationContext;

    public void fetchPage(Page page) {
        try {
            validateSafeSource(page.getSource());
            Fetcher fetcher = this.getFetcher(convert(page.getSource()));

            if (fetcher != null) {
                try {
                    final Resource resource = fetcher.fetch();
                    page.setContent(getResourceContentAsString(resource));
                    if (resource.getMetadata() != null) {
                        page.setMetadata(new HashMap<>(resource.getMetadata().size()));
                        for (Map.Entry<String, Object> entry : resource.getMetadata().entrySet()) {
                            if (!(entry.getValue() instanceof Map)) {
                                page.getMetadata().put(entry.getKey(), String.valueOf(entry.getValue()));
                            }
                        }
                    }
                    if (fetcher.getConfiguration().isAutoFetch()) {
                        page.setUseAutoFetch(Boolean.TRUE);
                    } else {
                        page.setUseAutoFetch(null); // set null to remove the value not set to false
                    }
                } catch (Exception ex) {
                    log.error("An error occurs while trying to create {}", page, ex);
                    throw new TechnicalManagementException("An error occurs while trying create " + page, ex);
                }
            }
        } catch (FetcherException ex) {
            log.error("An error occurs while trying to create {}", page, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + page, ex);
        }
    }

    private String getResourceContentAsString(final Resource resource) throws FetcherException {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getContent()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new FetcherException(e.getMessage(), e);
        }
    }

    private static PageSource convert(PageSource pageSourceEntity) {
        PageSource source = null;
        if (pageSourceEntity != null && pageSourceEntity.getType() != null && pageSourceEntity.getConfiguration() != null) {
            source = new PageSource();
            source.setType(pageSourceEntity.getType());
            source.setConfiguration(pageSourceEntity.getConfiguration());
        }
        return source;
    }

    @SuppressWarnings({ "Duplicates", "unchecked" })
    private Fetcher getFetcher(PageSource ps) throws FetcherException {
        if (ps == null || ps.getConfiguration().isEmpty()) {
            return null;
        }
        try {
            FetcherPlugin fetcherPlugin = fetcherPluginManager.get(ps.getType());
            ClassLoader fetcherCL = fetcherPlugin.fetcher().getClassLoader();
            Fetcher fetcher;
            if (fetcherPlugin.configuration().isAssignableFrom(FilepathAwareFetcherConfiguration.class)) {
                Class<? extends FetcherConfiguration> fetcherConfigurationClass =
                    (Class<? extends FetcherConfiguration>) fetcherCL.loadClass(fetcherPlugin.configuration().getName());
                Class<? extends FilesFetcher> fetcherClass = (Class<? extends FilesFetcher>) fetcherCL.loadClass(fetcherPlugin.clazz());
                FetcherConfiguration fetcherConfigurationInstance = fetcherConfigurationFactory.create(
                    fetcherConfigurationClass,
                    ps.getConfiguration()
                );
                fetcher = fetcherClass.getConstructor(fetcherConfigurationClass).newInstance(fetcherConfigurationInstance);
            } else {
                Class<? extends FetcherConfiguration> fetcherConfigurationClass =
                    (Class<? extends FetcherConfiguration>) fetcherCL.loadClass(fetcherPlugin.configuration().getName());
                Class<? extends Fetcher> fetcherClass = (Class<? extends Fetcher>) fetcherCL.loadClass(fetcherPlugin.clazz());
                FetcherConfiguration fetcherConfigurationInstance = fetcherConfigurationFactory.create(
                    fetcherConfigurationClass,
                    ps.getConfiguration()
                );
                fetcher = fetcherClass.getConstructor(fetcherConfigurationClass).newInstance(fetcherConfigurationInstance);
            }
            applicationContext.getAutowireCapableBeanFactory().autowireBean(fetcher);
            return fetcher;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new FetcherException(e.getMessage(), e);
        }
    }

    private void validateSafeSource(PageSource source) {
        if (importConfiguration.isAllowImportFromPrivate() || source == null || source.getConfiguration() == null) {
            return;
        }

        Map<String, String> map;

        try {
            map = new ObjectMapper().readValue(source.getConfiguration(), new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            throw new InvalidDataException("Source is invalid", e);
        }

        Optional<String> urlOpt = map
            .entrySet()
            .stream()
            .filter(e -> e.getKey().equals("repository") || e.getKey().matches(".*[uU]rl"))
            .map(Map.Entry::getValue)
            .findFirst();

        if (!urlOpt.isPresent()) {
            // There is no source to validate.
            return;
        }

        // Validate the url is allowed.
        UrlSanitizerUtils.checkAllowed(urlOpt.get(), importConfiguration.getImportWhitelist(), false);
    }
}
