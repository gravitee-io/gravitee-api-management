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
package io.gravitee.gamma.rest.infra.service_provider.gravitee_plugin;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.rest.core.gravitee_plugin.exception.PluginNotFoundException;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PluginMoreInformation;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PluginProvider;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.logging.NodeLoggerFactory;
import io.gravitee.plugin.core.api.ConfigurablePlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * Lists the plugins of one type, deployed or not, the way the Management API v2 plugin endpoints do: a plugin is
 * reported as deployed only when the node deployed it and the organization's license enables its feature.
 */
public abstract class AbstractPluginProvider<T extends ConfigurablePlugin> implements PluginProvider {

    protected final Logger log = NodeLoggerFactory.getLogger(this.getClass());

    protected final ConfigurablePluginManager<T> pluginManager;
    private final LicenseManager licenseManager;

    protected AbstractPluginProvider(ConfigurablePluginManager<T> pluginManager, LicenseManager licenseManager) {
        this.pluginManager = pluginManager;
        this.licenseManager = licenseManager;
    }

    @Override
    public Set<PlatformPlugin> findAll(String organizationId) {
        var license = licenseManager.getOrganizationLicenseOrPlatform(organizationId);
        return pluginManager
            .findAll(true)
            .stream()
            .map(plugin -> convert(plugin, license))
            .collect(Collectors.toSet());
    }

    @Override
    public PlatformPlugin findById(String organizationId, String pluginId) {
        return convert(getPlugin(pluginId), licenseManager.getOrganizationLicenseOrPlatform(organizationId));
    }

    @Override
    public String getSchema(String pluginId) {
        getPlugin(pluginId);
        try {
            return pluginManager.getSchema(pluginId, true);
        } catch (IOException e) {
            throw technicalError("schema", pluginId, e);
        }
    }

    @Override
    public String getIcon(String pluginId) {
        getPlugin(pluginId);
        return readIcon(pluginId);
    }

    @Override
    public String getDocumentation(String pluginId) {
        getPlugin(pluginId);
        try {
            return pluginManager.getDocumentation(pluginId, true);
        } catch (IOException e) {
            throw technicalError("documentation", pluginId, e);
        }
    }

    @Override
    public PluginMoreInformation getMoreInformation(String pluginId) {
        getPlugin(pluginId);
        try {
            var moreInformation = pluginManager.getMoreInformation(pluginId, true);
            if (moreInformation == null) {
                return new PluginMoreInformation(null, null, null);
            }
            return new PluginMoreInformation(
                moreInformation.getDescription(),
                moreInformation.getDocumentationUrl(),
                moreInformation.getSchemaImg()
            );
        } catch (IOException e) {
            throw technicalError("more information", pluginId, e);
        }
    }

    private T getPlugin(String pluginId) {
        var plugin = pluginManager.get(pluginId, true);
        if (plugin == null) {
            throw new PluginNotFoundException(pluginId);
        }
        return plugin;
    }

    private PlatformPlugin convert(T plugin, License license) {
        return new PlatformPlugin(
            plugin.id(),
            plugin.manifest().name(),
            plugin.manifest().description(),
            plugin.manifest().category(),
            plugin.manifest().version(),
            readIcon(plugin.id()),
            plugin.manifest().feature(),
            plugin.deployed() && license.isFeatureEnabled(plugin.manifest().feature())
        );
    }

    private String readIcon(String pluginId) {
        try {
            return pluginManager.getIcon(pluginId, true);
        } catch (IOException e) {
            throw technicalError("icon", pluginId, e);
        }
    }

    private TechnicalDomainException technicalError(String what, String pluginId, IOException cause) {
        log.error("Unable to read the {} of plugin {}", what, pluginId, cause);
        return new TechnicalDomainException("An error occurs while trying to get plugin " + what + " for plugin " + pluginId, cause);
    }
}
