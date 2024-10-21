package io.gravitee.gateway.standalone.secrets;

import com.graviteesource.services.runtimesecrets.api.discovery.DefinitionBrowser;
import com.graviteesource.services.runtimesecrets.discovery.browsers.ApiV4DefinitionBrowser;
import io.gravitee.definition.model.v4.Api;
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
