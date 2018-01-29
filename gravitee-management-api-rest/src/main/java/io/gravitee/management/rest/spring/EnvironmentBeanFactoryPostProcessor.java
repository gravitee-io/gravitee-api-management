package io.gravitee.management.rest.spring;


import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final static String PROPERTY_PREFIX = "gravitee.";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        StandardEnvironment environment = (StandardEnvironment) beanFactory.getBean(Environment.class);

        if (environment != null) {
            Map<String, Object> systemEnvironment = environment.getSystemEnvironment();
            Map<String, Object> prefixlessSystemEnvironment = new HashMap<>(systemEnvironment.size());
            systemEnvironment
                    .keySet()
                    .forEach(key -> {
                        String prefixKey = key;
                        if (key.startsWith(PROPERTY_PREFIX)) {
                            prefixKey = key.substring(PROPERTY_PREFIX.length());
                        }
                        prefixlessSystemEnvironment.put(prefixKey, systemEnvironment.get(key));
                    });
            SystemEnvironmentPropertySource systemEnvironmentPropertySource =
                    new SystemEnvironmentPropertySource(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, prefixlessSystemEnvironment);

            environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new PrefixAwarePropertySource(systemEnvironmentPropertySource));
        }
    }

    class PrefixAwarePropertySource extends PropertySource {


        private final PropertySource propertySource;

        public PrefixAwarePropertySource(PropertySource propertySource) {
            super(propertySource.getName());
            this.propertySource = propertySource;
        }

        @Override
        public boolean containsProperty(String name) {
            removePrefix(name);
            return propertySource.containsProperty(name);
        }

        @Override
        public Object getProperty(String name) {
            removePrefix(name);
            return propertySource.getProperty(name);
        }

        private String removePrefix(String name) {
            if (name == null) {
                return null;
            }

            if (name.startsWith(PROPERTY_PREFIX)) {
                return name.substring(PROPERTY_PREFIX.length());
            }

            return name;
        }
    }
}