package io.gravitee.gateway.standalone.secrets;

import io.gravitee.definition.model.services.secrets.ApiV4DefinitionBrowser;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.node.api.secrets.runtime.discovery.DefinitionBrowser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class DefinitionBrowserBeanFactory {

    @Bean
    DefinitionBrowser<Api> apiV4() {
        return new ApiV4DefinitionBrowser();
    }
}
