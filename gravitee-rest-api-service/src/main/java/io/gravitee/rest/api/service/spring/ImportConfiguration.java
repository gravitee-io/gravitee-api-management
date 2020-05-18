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
package io.gravitee.rest.api.service.spring;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ImportConfiguration implements InitializingBean {

    @Value("${imports.allow-from-private:true}")
    private boolean allowImportFromPrivate;

    private List<String> importWhitelist;

    @Autowired
    private Environment environment;

    @Override
    public void afterPropertiesSet() {

        int i = 0;
        importWhitelist = new ArrayList<>();

        String whitelistUrl;

        while ((whitelistUrl = environment.getProperty("imports.whitelist[" + i + "]")) != null) {
            importWhitelist.add(whitelistUrl);
            i++;
        }
    }

    public boolean isAllowImportFromPrivate() {
        return allowImportFromPrivate;
    }

    public List<String> getImportWhitelist() {
        return importWhitelist;
    }
}
