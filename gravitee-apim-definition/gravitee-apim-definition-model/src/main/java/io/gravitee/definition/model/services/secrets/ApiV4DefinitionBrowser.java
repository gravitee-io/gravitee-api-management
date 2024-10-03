package io.gravitee.definition.model.services.secrets;

import static io.gravitee.node.api.secrets.runtime.discovery.PayloadLocation.PLUGIN_KIND;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.node.api.secrets.runtime.discovery.Definition;
import io.gravitee.node.api.secrets.runtime.discovery.DefinitionBrowser;
import io.gravitee.node.api.secrets.runtime.discovery.DefinitionPayloadNotifier;
import io.gravitee.node.api.secrets.runtime.discovery.PayloadLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiV4DefinitionBrowser implements DefinitionBrowser<Api> {

    @Override
    public boolean canHandle(Object definition) {
        return definition != null && Objects.equals(definition.getClass(), Api.class);
    }

    @Override
    public Definition getDefinitionKindLocation(Api definition, Map<String, String> metadata) {
        return new Definition("api-v4", metadata.get("api_cross_id"), Optional.ofNullable(metadata.get("deployment_number")));
    }

    @Override
    public void findPayloads(Api definition, DefinitionPayloadNotifier notifier) {
        // listeners
        definition
            .getListeners()
            .stream()
            .flatMap(l -> l.getEntrypoints().stream())
            .forEach(entrypoint -> {
                String payload = entrypoint.getConfiguration();
                notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, entrypoint.getType()), entrypoint::setConfiguration);
            });

        // resources
        definition
            .getResources()
            .forEach(resource -> {
                String payload = resource.getConfiguration();
                notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, resource.getType()), resource::setConfiguration);
            });

        // flows api and plan
        List<Flow> flows = definition
            .getPlans()
            .stream()
            .flatMap(p -> p.getFlows().stream())
            .collect(Collectors.toCollection(ArrayList::new));
        flows.addAll(definition.getFlows());
        Stream
            .concat(
                Stream.concat(
                    flows.stream().flatMap(flow -> flow.getRequest().stream()),
                    flows.stream().flatMap(flow -> flow.getResponse().stream())
                ),
                Stream.concat(
                    flows.stream().flatMap(flow -> flow.getPublish().stream()),
                    flows.stream().flatMap(flow -> flow.getSubscribe().stream())
                )
            )
            .forEach(step -> {
                var payload = step.getConfiguration();
                notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, step.getPolicy()), step::setConfiguration);
            });

        definition
            .getPlans()
            .forEach(plan -> {
                PlanSecurity security = plan.getSecurity();
                String payload = security.getConfiguration();
                notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, security.getType()), security::setConfiguration);
            });

        // endpoint groups
        definition
            .getEndpointGroups()
            .stream()
            .flatMap(endpointGroup -> {
                EndpointGroupServices services = endpointGroup.getServices();
                Stream
                    .of(services.getDiscovery(), services.getHealthCheck())
                    .filter(Service::isEnabled)
                    .forEach(service -> {
                        String payload = service.getConfiguration();
                        notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, service.getType()), service::setConfiguration);
                    });
                String payload = endpointGroup.getSharedConfiguration();
                notifier.onPayload(
                    payload,
                    new PayloadLocation(PLUGIN_KIND, endpointGroup.getType()),
                    endpointGroup::setSharedConfiguration
                );
                return endpointGroup.getEndpoints().stream();
            })
            .forEach(endpoint -> {
                EndpointServices services = endpoint.getServices();
                Stream
                    .of(services.getHealthCheck())
                    .filter(Service::isEnabled)
                    .forEach(service -> {
                        String payload = service.getConfiguration();
                        notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, service.getType()), service::setConfiguration);
                    });
                String payload = endpoint.getConfiguration();
                notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, endpoint.getType()), endpoint::setConfiguration);
                payload = endpoint.getSharedConfigurationOverride();
                notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, endpoint.getType()), endpoint::setSharedConfigurationOverride);
            });

        // services
        Service dynamicProperty = definition.getServices().getDynamicProperty();
        String payload = dynamicProperty.getConfiguration();
        notifier.onPayload(payload, new PayloadLocation(PLUGIN_KIND, dynamicProperty.getType()), dynamicProperty::setConfiguration);
    }
}
