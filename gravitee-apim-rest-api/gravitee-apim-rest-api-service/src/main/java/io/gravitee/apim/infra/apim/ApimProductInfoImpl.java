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
package io.gravitee.apim.infra.apim;

import io.gravitee.apim.core.apim.service_provider.ApimProductInfo;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Properties;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Getter
@Service
public class ApimProductInfoImpl implements ApimProductInfo {

    String name;
    String version;

    @PostConstruct
    void init() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("api.properties"));

        name = properties.getProperty("api.name");
        version = properties.getProperty("api.version");
    }
}
