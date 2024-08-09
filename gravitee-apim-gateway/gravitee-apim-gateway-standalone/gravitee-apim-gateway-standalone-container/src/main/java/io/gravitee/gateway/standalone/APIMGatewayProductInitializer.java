package io.gravitee.gateway.standalone;

import io.gravitee.node.container.GraviteeProductInitializer;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class APIMGatewayProductInitializer implements GraviteeProductInitializer {

    @Override
    public String productName() {
        return "apim-gateway";
    }
}
