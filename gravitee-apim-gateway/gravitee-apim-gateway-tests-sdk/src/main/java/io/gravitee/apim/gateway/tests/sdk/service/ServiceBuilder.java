package io.gravitee.apim.gateway.tests.sdk.service;

import io.gravitee.common.service.AbstractService;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServiceBuilder {

    public static Plugin build(Class<? extends AbstractService<?>> serviceClass) {
        return new TestServicePlugin(serviceClass);
    }

    private static class TestServicePlugin implements Plugin {

        Class<? extends AbstractService<?>> serviceClass;

        TestServicePlugin(Class<? extends AbstractService<?>> serviceClass) {
            this.serviceClass = serviceClass;
        }

        @Override
        public String id() {
            return "test-service-".concat(serviceClass.getName());
        }

        @Override
        public String clazz() {
            return serviceClass.getName();
        }

        @Override
        public String type() {
            return "service";
        }

        @Override
        public Path path() {
            return null;
        }

        @Override
        public PluginManifest manifest() {
            return null;
        }

        @Override
        public URL[] dependencies() {
            return new URL[0];
        }

        @Override
        public boolean deployed() {
            return true;
        }
    }
}
