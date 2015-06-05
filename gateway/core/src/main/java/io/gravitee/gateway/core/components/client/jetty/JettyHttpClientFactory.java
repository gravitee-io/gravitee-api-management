package io.gravitee.gateway.core.components.client.jetty;

import io.gravitee.gateway.core.components.client.HttpClientFactory;
import io.gravitee.model.Api;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyHttpClientFactory implements HttpClientFactory<JettyHttpClient> {

    private Map<Api, JettyHttpClient> clients = new HashMap();

    @Override
    public JettyHttpClient create(Api api) {
        synchronized (clients) {
            JettyHttpClient client = clients.get(api);
            if (client == null) {
                client = new JettyHttpClient(api);
                clients.put(api, client);
            }
            return client;
        }
    }
}
