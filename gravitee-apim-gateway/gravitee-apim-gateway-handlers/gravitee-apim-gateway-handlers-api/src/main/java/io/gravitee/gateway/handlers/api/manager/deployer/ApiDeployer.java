package io.gravitee.gateway.handlers.api.manager.deployer;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.Deployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeployer implements Deployer<Api> {

    private final Logger logger = LoggerFactory.getLogger(ApiDeployer.class);

    private final GatewayConfiguration gatewayConfiguration;

    private final DataEncryptor dataEncryptor;

    public ApiDeployer(GatewayConfiguration gatewayConfiguration, DataEncryptor dataEncryptor) {
        this.gatewayConfiguration = gatewayConfiguration;
        this.dataEncryptor = dataEncryptor;
    }

    @Override
    public void prepare(Api api) {
        api.getDefinition().setPlans(getPlansMatchingShardingTag(api));
        decryptProperties(api.getDefinition().getProperties());
    }

    @Override
    public List<String> getPlans(Api api) {
        return api.getDefinition().getPlans().stream().map(Plan::getName).collect(Collectors.toList());
    }

    private List<Plan> getPlansMatchingShardingTag(Api api) {
        return api
                .getDefinition()
                .getPlans()
                .stream()
                .filter(
                        plan -> {
                            if (plan.getTags() != null && !plan.getTags().isEmpty()) {
                                boolean hasMatchingTags = gatewayConfiguration.hasMatchingTags(plan.getTags());
                                if (!hasMatchingTags) {
                                    logger.debug(
                                            "Plan name[{}] api[{}] has been ignored because not in configured sharding tags",
                                            plan.getName(),
                                            api.getName()
                                    );
                                }
                                return hasMatchingTags;
                            }
                            return true;
                        }
                )
                .collect(Collectors.toList());
    }

    private void decryptProperties(Properties properties) {
        if (properties != null) {
            for (Property property : properties.getProperties()) {
                if (property.isEncrypted()) {
                    try {
                        property.setValue(dataEncryptor.decrypt(property.getValue()));
                        property.setEncrypted(false);
                        properties.getValues().put(property.getKey(), property.getValue());
                    } catch (GeneralSecurityException e) {
                        logger.error("Error decrypting API property value for key {}", property.getKey(), e);
                    }
                }
            }
        }
    }
}
