package io.gravitee.management.api.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Properties;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
public class PropertiesConfiguration {

    protected final static Logger LOGGER = LoggerFactory.getLogger(PropertiesConfiguration.class);

    public final static String GRAVITEE_CONFIGURATION = "gravitee.conf";

    @Bean(name = "graviteeProperties")
    public static Properties graviteeProperties() throws IOException {
        LOGGER.info("Loading Gravitee Management configuration.");

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

        String yamlConfiguration = System.getProperty(GRAVITEE_CONFIGURATION);
    //    Resource yamlResource = new FileSystemResource(yamlConfiguration);

    //    LOGGER.info("\tGravitee Management configuration loaded from {}", yamlResource.getURL().getPath());

        //   yaml.setResources(yamlResource);
        Properties properties = yaml.getObject();
        LOGGER.info("Loading Gravitee Management configuration. DONE");

        return properties;

    }
}