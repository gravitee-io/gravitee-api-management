package io.gravitee.rest.api.management.v2.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.Sensitive;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.fetcher.impl.FetcherConfigurationFactoryImpl;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper
public abstract class ConfigurationSerializationMapper {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSerializationMapper.class);
    private static final String SENSITIVE_DATA_REPLACEMENT = "********";

    @Named("serializeConfiguration")
    public String serializeConfiguration(Object configuration) {
        if (Objects.isNull(configuration)) {
            return null;
        }
        if (configuration instanceof LinkedHashMap) {
            ObjectMapper mapper = new GraviteeMapper();
            try {
                JsonNode jsonNode = mapper.valueToTree(configuration);
                return mapper.writeValueAsString(jsonNode);
            } catch (IllegalArgumentException | JsonProcessingException e) {
                throw new TechnicalManagementException("An error occurred while trying to parse configuration " + e);
            }
        } else {
            return configuration.toString();
        }
    }

    @Named("convertToMapConfiguration")
    public Map<String, Object> convertToMapConfiguration(Object configuration) {
        if (Objects.isNull(configuration)) {
            return Map.of();
        }
        if (configuration instanceof LinkedHashMap) {
            return (Map<String, Object>) configuration;
        } else {
            return Map.of();
        }
    }

    @Named("deserializeConfiguration")
    public Object deserializeConfiguration(String configuration) {
        if (Objects.isNull(configuration)) {
            return null;
        }

        ObjectMapper mapper = new GraviteeMapper();
        try {
            return mapper.readValue(configuration, LinkedHashMap.class);
        } catch (JsonProcessingException jse) {
            logger.debug("Cannot parse configuration as LinkedHashMap: " + configuration);
        }

        try {
            return mapper.readValue(configuration, List.class);
        } catch (JsonProcessingException jse) {
            logger.debug("Cannot parse configuration as List: " + configuration);
        }

        return configuration;
    }

    @Named("deserializeConfigurationWithSensitiveDataMasking")
    public Object deserializeConfigurationWithSensitiveDataMasking(String configuration, String fetcherType) {
        if (Objects.isNull(configuration)) {
            return null;
        }

        ObjectMapper mapper = new GraviteeMapper();
        try {
            LinkedHashMap<String, Object> configMap = mapper.readValue(configuration, LinkedHashMap.class);

            if (fetcherType != null) {
                // Try annotation-based masking with FetcherConfigurationFactory
                try {
                    Class<? extends FetcherConfiguration> fetcherConfigClass = getFetcherConfigurationClass(fetcherType);
                    if (fetcherConfigClass != null) {
                        // Create factory instance
                        FetcherConfigurationFactory factory = new FetcherConfigurationFactoryImpl();

                        // Convert to actual configuration object
                        FetcherConfiguration configObj = factory.create(fetcherConfigClass, configuration);

                        if (configObj != null) {
                            // Mask sensitive fields using annotations
                            maskSensitiveFields(configObj);

                            // Convert back to map
                            String maskedJson = mapper.writeValueAsString(configObj);
                            return mapper.readValue(maskedJson, LinkedHashMap.class);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Annotation-based masking failed for fetcher type: " + fetcherType, e);
                }
            }

            // Fallback to field name-based masking
            maskSensitiveFieldsInMap(configMap);
            return configMap;
        } catch (JsonProcessingException e) {
            logger.debug("Cannot parse configuration: " + e.getMessage());
        }

        return configuration;
    }

    /**
     * Get the fetcher configuration class based on fetcher type.
     */
    private Class<? extends FetcherConfiguration> getFetcherConfigurationClass(String fetcherType) {
        switch (fetcherType.toLowerCase()) {
            case "github":
                return getClassSafely("io.gravitee.fetcher.github.GitHubFetcherConfiguration");
            case "gitlab":
                return getClassSafely("io.gravitee.fetcher.gitlab.GitLabFetcherConfiguration");
            case "git":
                return getClassSafely("io.gravitee.fetcher.git.GitFetcherConfiguration");
            case "http":
            case "http-fetcher":
                return getClassSafely("io.gravitee.fetcher.http.HttpFetcherConfiguration");
            default:
                logger.debug("Unknown fetcher type: " + fetcherType);
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends FetcherConfiguration> getClassSafely(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (FetcherConfiguration.class.isAssignableFrom(clazz)) {
                return (Class<? extends FetcherConfiguration>) clazz;
            }
        } catch (ClassNotFoundException e) {
            logger.debug("Fetcher configuration class not found: " + className);
        }
        return null;
    }

    /**
     * Mask sensitive fields in a configuration object using @Sensitive annotations.
     */
    private void maskSensitiveFields(Object configuration) {
        Class<?> configClass = configuration.getClass();
        Field[] fields = configClass.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Sensitive.class)) {
                try {
                    field.setAccessible(true);
                    field.set(configuration, SENSITIVE_DATA_REPLACEMENT);
                } catch (IllegalAccessException e) {
                    logger.debug("Could not mask sensitive field: " + field.getName(), e);
                }
            }
        }
    }

    /**
     * Fallback method to mask sensitive fields by field name.
     */
    private void maskSensitiveFieldsInMap(LinkedHashMap<String, Object> configMap) {
        String[] sensitiveFieldNames = {
            "privateToken", // GitLab fetcher
            "password", // HTTP, Git fetchers
            "secret", // Various fetchers
            "token", // Various fetchers
            "apiKey", // API fetchers
            "accessToken", // OAuth fetchers
            "clientSecret", // OAuth fetchers
            "personalAccessToken", // GitHub, GitLab
            "authToken", // Generic auth tokens
        };

        for (String fieldName : sensitiveFieldNames) {
            if (configMap.containsKey(fieldName)) {
                configMap.put(fieldName, SENSITIVE_DATA_REPLACEMENT);
            }
        }
    }
}
