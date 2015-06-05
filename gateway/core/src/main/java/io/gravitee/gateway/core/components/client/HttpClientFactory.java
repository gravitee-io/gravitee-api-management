package io.gravitee.gateway.core.components.client;

import io.gravitee.model.Api;

/**
 * An HTTP client factory
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface HttpClientFactory<T extends HttpClient> {

    T create(Api api);
}
