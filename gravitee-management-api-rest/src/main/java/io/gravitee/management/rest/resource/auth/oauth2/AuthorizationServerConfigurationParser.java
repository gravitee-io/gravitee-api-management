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
package io.gravitee.management.rest.resource.auth.oauth2;

import io.gravitee.common.util.EnvironmentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Christophe LANNOY (chrislannoy.java at gmail.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 */
public class AuthorizationServerConfigurationParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationServerConfigurationParser.class);

    public ServerConfiguration parseConfiguration(Map<String, Object> configuration) {
        ServerConfiguration serverConfiguration = parseServerConfiguration(configuration);
        serverConfiguration.setGroupsMapping(getMappings(configuration, "groups"));
        serverConfiguration.setRolesMapping(getMappings(configuration, "roles"));
        serverConfiguration.setUserMapping(parseUserMapping(configuration));

        return serverConfiguration;
    }

    private ServerConfiguration parseServerConfiguration(Map<String, Object> configuration) {
        ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setClientId((String)EnvironmentUtils.get("clientId", configuration));
        serverConfiguration.setClientSecret((String)EnvironmentUtils.get("clientSecret", configuration));
        serverConfiguration.setTokenEndpoint((String)EnvironmentUtils.get("tokenEndpoint", configuration));
        serverConfiguration.setAccessTokenProperty((String)EnvironmentUtils.get("accessTokenProperty", configuration));
        serverConfiguration.setUserInfoEndpoint((String)EnvironmentUtils.get("userInfoEndpoint", configuration));
        serverConfiguration.setAuthorizationHeader((String)EnvironmentUtils.get("authorizationHeader", configuration));
        serverConfiguration.setTokenIntrospectionEndpoint((String)EnvironmentUtils.get("tokenIntrospectionEndpoint", configuration));

        return serverConfiguration;
    }


    private UserMapping parseUserMapping(Map<String, Object> configuration) {
        UserMapping userMapping = new UserMapping();
        userMapping.setEmail((String)EnvironmentUtils.get("mapping.email", configuration));
        userMapping.setId((String)EnvironmentUtils.get("mapping.id", configuration));
        userMapping.setLastname((String)EnvironmentUtils.get("mapping.lastname", configuration));
        userMapping.setFirstname((String)EnvironmentUtils.get("mapping.firstname", configuration));
        userMapping.setPicture((String)EnvironmentUtils.get("mapping.picture", configuration));
        Boolean emailRequired = (Boolean) EnvironmentUtils.get("mapping.emailRequired", configuration);
        userMapping.setEmailRequired(emailRequired == null || emailRequired);
        userMapping.setUsername((String)EnvironmentUtils.get("mapping.username", configuration));

        return userMapping;
    }

    private List<ExpressionMapping> getMappings(Map<String, Object> configuration, String prefix) {
        List<ExpressionMapping> result = new ArrayList<>();

        int idx = 0;
        boolean found = true;

        while(found) {

            String path = prefix + "[" + idx + "].mapping";
            String condition = (String) EnvironmentUtils.get(path +".condition", configuration);

            if(!StringUtils.isEmpty(condition)) {
                List<String> rolesName = parseMappingValues(configuration, path);

                ExpressionMapping mapping = new ExpressionMapping(condition.trim(), rolesName);

                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Expression {} give roles {}", mapping.getCondition(), mapping.getValues().toString());
                }

                result.add(mapping);
                idx++;
            } else {
                found = false;
            }
        }

        return result;
    }

    private List<String> parseMappingValues(Map<String, Object> configuration, String path) {

        List<String> result = new ArrayList<>();

        int idx = 0;
        boolean found = true;

        while(found) {
            String valueName = (String)EnvironmentUtils.get(path + ".values[" + idx + "]", configuration);
            if(!StringUtils.isEmpty(valueName)) {
                result.add(valueName.trim());
                idx++;
            } else {
                found = false;
            }
        }

        return result;
    }

}
