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
package inmemory;

import io.gravitee.apim.core.documentation.domain_service.PageSourceDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageSourceException;
import io.gravitee.apim.core.documentation.model.Page;
import java.util.Map;
import org.springframework.scheduling.support.CronExpression;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageSourceDomainServiceInMemory implements PageSourceDomainService {

    public static final String MARKDOWN = "# In memory markdown";

    @Override
    public void setContentFromSource(Page page) {
        if (page.getSource() != null) {
            page.setContent(MARKDOWN);
        }
    }
}
