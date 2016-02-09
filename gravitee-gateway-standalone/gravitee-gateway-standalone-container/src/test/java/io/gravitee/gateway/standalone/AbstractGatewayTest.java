package io.gravitee.gateway.standalone;

import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.gravitee.gateway.standalone.junit.rules.ApiPublisher;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractGatewayTest {

    @ClassRule
    public static final TestRule chain = RuleChain
            .outerRule(new ApiPublisher())
            .around(new ApiDeployer());

    /*
    @ClassRule
    public final TestRule apiChain = RuleChain
            .outerRule(new ApiPublisher())
            .around(new ApiDeployer());
            */
}
