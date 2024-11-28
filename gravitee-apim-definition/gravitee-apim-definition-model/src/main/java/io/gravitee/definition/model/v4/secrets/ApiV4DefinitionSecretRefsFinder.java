package io.gravitee.definition.model.v4.secrets;

import static io.gravitee.secrets.api.discovery.SecretRefsLocation.PLUGIN_KIND;

import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsListener;
import io.gravitee.secrets.api.discovery.SecretRefsLocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiV4DefinitionSecretRefsFinder implements DefinitionSecretRefsFinder<Api> {

    public static final String RESPONSE_TEMPLATES_KIND = "response-templates";

    @Override
    public boolean canHandle(Object definition) {
        return definition != null && Api.class.isAssignableFrom(definition.getClass());
    }

    @Override
    public DefinitionDescriptor toDefinitionDescriptor(Api definition, DefinitionMetadata metadata) {
        return new DefinitionDescriptor(new Definition("api-v4", definition.getId()), Optional.ofNullable(metadata.revision()));
    }

    @Override
    public void findSecretRefs(Api definition, DefinitionSecretRefsListener listener) {
        // listeners
        safeStream(definition.getListeners())
            .flatMap(l -> safeStream(l.getEntrypoints()))
            .forEach(entrypoint ->
                listener.onCandidate(
                    entrypoint.getConfiguration(),
                    new SecretRefsLocation(PLUGIN_KIND, entrypoint.getType()),
                    entrypoint::setConfiguration
                )
            );

        // resources
        safeStream(definition.getResources())
            .forEach(resource ->
                listener.onCandidate(
                    resource.getConfiguration(),
                    new SecretRefsLocation(PLUGIN_KIND, resource.getType()),
                    resource::setConfiguration
                )
            );

        // flows api and plan
        List<Flow> flows = safeList(definition.getPlans())
            .stream()
            .flatMap(p -> safeStream(p.getFlows()))
            .collect(Collectors.toCollection(ArrayList::new));
        flows.addAll(safeList(definition.getFlows()));
        Stream
            .concat(
                Stream.concat(
                    flows.stream().flatMap(flow -> safeStream(flow.getRequest())),
                    flows.stream().flatMap(flow -> safeStream(flow.getResponse()))
                ),
                Stream.concat(
                    flows.stream().flatMap(flow -> safeStream(flow.getPublish())),
                    flows.stream().flatMap(flow -> safeStream(flow.getSubscribe()))
                )
            )
            .forEach(step ->
                listener.onCandidate(step.getConfiguration(), new SecretRefsLocation(PLUGIN_KIND, step.getPolicy()), step::setConfiguration)
            );

        safeStream(definition.getPlans())
            .forEach(plan ->
                Optional
                    .ofNullable(plan.getSecurity())
                    .ifPresent(security ->
                        listener.onCandidate(
                            security.getConfiguration(),
                            new SecretRefsLocation(PLUGIN_KIND, security.getType()),
                            security::setConfiguration
                        )
                    )
            );

        // endpoint groups
        safeStream(definition.getEndpointGroups())
            .flatMap(endpointGroup -> {
                EndpointGroupServices services = endpointGroup.getServices();
                List<Service> list = new ArrayList<>();
                if (services.getDiscovery() != null) {
                    list.add(services.getDiscovery());
                }
                if (services.getHealthCheck() != null) {
                    list.add(services.getHealthCheck());
                }
                list
                    .stream()
                    .filter(Service::isEnabled)
                    .forEach(service ->
                        listener.onCandidate(
                            service.getConfiguration(),
                            new SecretRefsLocation(PLUGIN_KIND, service.getType()),
                            service::setConfiguration
                        )
                    );
                Optional
                    .ofNullable(endpointGroup.getSharedConfiguration())
                    .ifPresent(payload ->
                        listener.onCandidate(
                            payload,
                            new SecretRefsLocation(PLUGIN_KIND, endpointGroup.getType()),
                            endpointGroup::setSharedConfiguration
                        )
                    );
                return safeStream(endpointGroup.getEndpoints());
            })
            .forEach(endpoint -> {
                Optional
                    .ofNullable(endpoint.getServices())
                    .map(EndpointServices::getHealthCheck)
                    .filter(Service::isEnabled)
                    .ifPresent(service ->
                        listener.onCandidate(
                            service.getConfiguration(),
                            new SecretRefsLocation(PLUGIN_KIND, service.getType()),
                            service::setConfiguration
                        )
                    );
                listener.onCandidate(
                    endpoint.getConfiguration(),
                    new SecretRefsLocation(PLUGIN_KIND, endpoint.getType()),
                    endpoint::setConfiguration
                );
                listener.onCandidate(
                    endpoint.getSharedConfigurationOverride(),
                    new SecretRefsLocation(PLUGIN_KIND, endpoint.getType()),
                    endpoint::setSharedConfigurationOverride
                );
            });

        // services
        Optional
            .ofNullable(definition.getServices())
            .map(ApiServices::getDynamicProperty)
            .ifPresent(dynamicProperty ->
                listener.onCandidate(
                    dynamicProperty.getConfiguration(),
                    new SecretRefsLocation(PLUGIN_KIND, dynamicProperty.getType()),
                    dynamicProperty::setConfiguration
                )
            );

        // response templates
        Map<String, Map<String, ResponseTemplate>> responseTemplates = definition.getResponseTemplates();
        safeKeySetStream(responseTemplates)
            .forEach(key ->
                safeKeySetStream(responseTemplates.get(key))
                    .forEach(contentType -> {
                        ResponseTemplate responseTemplate = responseTemplates.get(key).get(contentType);
                        Optional
                            .ofNullable(responseTemplate.getBody())
                            .ifPresent(body ->
                                listener.onCandidate(body, new SecretRefsLocation(RESPONSE_TEMPLATES_KIND, key), responseTemplate::setBody)
                            );
                        safeKeySetStream(responseTemplate.getHeaders())
                            .forEach(headerName ->
                                listener.onCandidate(
                                    responseTemplate.getHeaders().get(headerName),
                                    new SecretRefsLocation(RESPONSE_TEMPLATES_KIND, key),
                                    updated -> responseTemplate.getHeaders().put(headerName, updated)
                                )
                            );
                    })
            );
    }

    private <T> List<T> safeList(List<T> col) {
        return col == null ? List.of() : col;
    }

    public <T> Stream<T> safeStream(Collection<T> col) {
        return col == null ? Stream.empty() : col.stream();
    }

    public <K, V> Stream<K> safeKeySetStream(Map<K, V> map) {
        return map == null ? Stream.empty() : map.keySet().stream();
    }
}
