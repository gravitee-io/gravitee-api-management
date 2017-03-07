package io.gravitee.management.service.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class EnvironmentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUtils.class);

    public static Map<String, Object> getPropertiesStartingWith(ConfigurableEnvironment aEnv, String aKeyPrefix) {
        Map<String,Object> result = new HashMap<>();
        Map<String,Object> map = getAllProperties( aEnv );

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith(aKeyPrefix)) {
                result.put(key, entry.getValue());
            }
        }

        return result;
    }

    public static Map<String,Object> getAllProperties(ConfigurableEnvironment aEnv) {
        Map<String,Object> result = new HashMap<>();
        aEnv.getPropertySources().forEach(ps -> addAll(result, getAllProperties(ps)));
        return result;
    }

    public static Map<String,Object> getAllProperties(PropertySource<?> aPropSource) {
        Map<String,Object> result = new HashMap<>();

        if (aPropSource instanceof CompositePropertySource) {
            CompositePropertySource cps = (CompositePropertySource) aPropSource;
            cps.getPropertySources().forEach(ps -> addAll(result, getAllProperties(ps)));
            return result;
        }

        if (aPropSource instanceof EnumerablePropertySource<?>) {
            EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) aPropSource;
            Arrays.asList(ps.getPropertyNames()).forEach(key -> result.put(key, ps.getProperty(key)));
            return result;
        }

        // note: Most descendants of PropertySource are EnumerablePropertySource. There are some
        // few others like JndiPropertySource or StubPropertySource
        LOGGER.debug( "Given PropertySource is instanceof " + aPropSource.getClass().getName()
                + " and cannot be iterated" );

        return result;
    }

    private static void addAll(Map<String, Object> aBase, Map<String, Object> aToBeAdded) {
        for (Map.Entry<String, Object> entry : aToBeAdded.entrySet()) {
            if (aBase.containsKey(entry.getKey())) {
                continue;
            }

            aBase.put(entry.getKey(), entry.getValue());
        }
    }
}
