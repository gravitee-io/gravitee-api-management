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
package io.gravitee.gateway.reactor.handler.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.command.ApiDeploymentFailureCommand;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.CommandTags;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.MessageRecipient;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class DefaultReactorHandlerRegistry implements ReactorHandlerRegistry {

    private final ReactorFactoryManager reactorFactoryManager;
    private final CommandRepository commandRepository;
    private final Node node;
    private final Map<Reactable, List<ReactableAcceptors>> reactables = new ConcurrentHashMap<>();
    private final Map<Class<? extends Acceptor<?>>, List<Acceptor<?>>> acceptors = new ConcurrentHashMap<>();
    private final Map<Class<? extends Acceptor<?>>, Class<? extends Acceptor<?>>> acceptorsClassMapping = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void create(Reactable reactable) {
        log.debug("Creating a new handler for {}", reactable);

        List<ReactorHandler> reactorHandlers = prepare(reactable);
        if (!reactorHandlers.isEmpty()) {
            reactorHandlers.forEach(reactorHandler -> register(reactable, reactorHandler));
        }
    }

    private List<ReactorHandler> prepare(Reactable reactable) {
        log.debug("Preparing a new reactor handler for: {}", reactable);
        List<ReactorHandler> reactorHandlers = reactorFactoryManager.create(reactable);
        List<ReactorHandler> startedReactorHandlers = new ArrayList<>();
        if (reactorHandlers != null) {
            reactorHandlers.forEach(reactorHandler -> {
                try {
                    reactorHandler.start();
                    startedReactorHandlers.add(reactorHandler);
                } catch (Exception ex) {
                    log.error("Unable to start the new reactor handler: {}", reactorHandler, ex);

                    if (reactable instanceof ReactableApi<?>) {
                        sendDeploymentCommand((ReactableApi<?>) reactable, ex);
                    }
                }
            });
        }

        return startedReactorHandlers;
    }

    private void sendDeploymentCommand(ReactableApi<?> reactableApi, Throwable throwable) {
        final Command command = new Command();
        Instant now = Instant.now();
        command.setId(UUID.random().toString());
        command.setFrom(node.id());
        command.setTo(MessageRecipient.MANAGEMENT_APIS.name());
        command.setTags(List.of(CommandTags.API_DEPLOYMENT_FAILURE.name()));
        command.setCreatedAt(Date.from(now));
        command.setUpdatedAt(Date.from(now));
        command.setEnvironmentId(reactableApi.getEnvironmentId());

        try {
            convertCommand(reactableApi, command, throwable);
            commandRepository.create(command);
        } catch (Exception cmdEx) {
            log.warn(
                "Unable to report a deployment failure for id[{}] name[{}] event [{}]",
                reactableApi.getId(),
                reactableApi.getName(),
                reactableApi.getEventId(),
                cmdEx
            );
        }
    }

    private void convertCommand(ReactableApi<?> reactableApi, Command command, Throwable throwable) {
        try {
            command.setContent(
                mapper.writeValueAsString(
                    new ApiDeploymentFailureCommand(node.id(), reactableApi.getEventId(), reactableApi.getId(), throwable.getMessage())
                )
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to convert api deployment failure command [{}] to string", reactableApi.getId(), e);
            JsonObject json = new JsonObject();
            json
                .put("nodeId", node.id())
                .put("apiId", reactableApi.getId())
                .put("eventId", reactableApi.getEventId())
                .put("failureCause", throwable.getMessage());
            command.setContent(json.encode());
        }
    }

    private void register(Reactable reactable, ReactorHandler handler) {
        log.debug("Registering a new handler: {}", handler);

        // Associate the handler to the acceptors
        List<Acceptor<?>> handlerAcceptors = handler.acceptors();

        List<ReactableAcceptors> reactableAcceptors = reactables.getOrDefault(reactable, new ArrayList<>());
        reactableAcceptors.add(new ReactableAcceptors(handler, handlerAcceptors));
        reactables.put(reactable, reactableAcceptors);

        registerAcceptors(handlerAcceptors);
    }

    @Override
    public void update(Reactable reactable) {
        log.debug("Updating handler for: {}", reactable);

        List<ReactableAcceptors> reactableAcceptors = reactables.get(reactable);

        if (reactableAcceptors != null && !reactableAcceptors.isEmpty()) {
            if (log.isDebugEnabled()) {
                reactableAcceptors.forEach(reactableHttpAcceptor -> {
                    log.debug("Handler is already deployed: {}", reactableHttpAcceptor.handler);
                });
            }

            List<ReactorHandler> newReactorHandlers = prepare(reactable);

            // Do not update handler if the new ones are not correctly initialized
            if (!newReactorHandlers.isEmpty()) {
                // Remove any handlers for the current reactable
                List<ReactableAcceptors> previousReactableAcceptors = reactables.remove(reactable);

                // Register the new handler before removing the previous http acceptor to avoid 404, especially on high throughput.
                newReactorHandlers.forEach(reactorHandler -> register(reactable, reactorHandler));

                removeAcceptors(reactable, previousReactableAcceptors);
            }
        } else {
            create(reactable);
        }
    }

    @Override
    public void remove(Reactable reactable) {
        final List<ReactableAcceptors> reactableAcceptors = reactables.get(reactable);
        removeAcceptors(reactable, reactableAcceptors, true);
    }

    @Override
    public void clear() {
        Iterator<Map.Entry<Reactable, List<ReactableAcceptors>>> reactableIte = reactables.entrySet().iterator();
        while (reactableIte.hasNext()) {
            final Map.Entry<Reactable, List<ReactableAcceptors>> next = reactableIte.next();
            removeAcceptors(next.getKey(), next.getValue(), false);
            reactableIte.remove();
        }
    }

    @Override
    public boolean contains(Reactable reactable) {
        return reactables.containsKey(reactable);
    }

    private void removeAcceptors(
        final Reactable reactable,
        final List<ReactableAcceptors> reactableAcceptors,
        final boolean removeReactable
    ) {
        if (reactableAcceptors != null) {
            try {
                removeAcceptors(reactable, reactableAcceptors);

                if (removeReactable) {
                    reactables.remove(reactable);
                }
                log.debug("Handler has been unregistered from the proxy");
            } catch (Exception e) {
                log.error("Unable to un-register handler", e);
            }
        }
    }

    private void removeAcceptors(final Reactable reactable, final List<ReactableAcceptors> reactableAcceptors) {
        // Remove the http acceptors before stopping the handler to avoid 500 errors.
        List<? extends Acceptor<?>> handlersAcceptors = reactableAcceptors
            .stream()
            .flatMap(reactableAcceptor -> reactableAcceptor.acceptors.stream())
            .toList();

        unregisterAcceptors(handlersAcceptors);

        reactableAcceptors.forEach(reactableHttpAcceptor -> {
            try {
                log.debug("Stopping previous handler for: {}", reactable);
                reactableHttpAcceptor.handler.stop();
            } catch (Exception ex) {
                log.error("Unable to stop handler", ex);
            }
        });
    }

    @Override
    public <T extends Acceptor<T>> Collection<T> getAcceptors(Class<T> acceptorType) {
        Collection<T> acceptorsType = (Collection<T>) acceptors.get(acceptorType);

        if (acceptorsType == null) {
            return Collections.emptyList();
        }

        return acceptorsType;
    }

    private Class<? extends Acceptor<?>> resolve(Class<? extends Acceptor> acceptor) {
        return acceptorsClassMapping.computeIfAbsent((Class<? extends Acceptor<?>>) acceptor, aClass -> {
            Class<?>[] acceptorClasses = aClass.getInterfaces();
            for (Class<?> acceptorClass : acceptorClasses) {
                if (Acceptor.class.isAssignableFrom(acceptorClass)) {
                    return (Class<? extends Acceptor<?>>) acceptorClass;
                }
            }

            return null;
        });
    }

    private void registerAcceptors(List<? extends Acceptor<?>> newAcceptors) {
        if (!newAcceptors.isEmpty()) {
            synchronized (this) {
                newAcceptors.forEach(acceptor -> {
                    Class<? extends Acceptor<?>> acceptorType = resolve(acceptor.getClass());
                    if (acceptorType != null) {
                        acceptors.compute(acceptorType, (k, v) -> {
                            var copy = v == null ? new ArrayList<Acceptor<?>>() : new ArrayList<>(v);
                            copy.add(acceptor);
                            copy.sort(null);
                            return Collections.unmodifiableList(copy);
                        });
                    }
                });
            }
        }
    }

    private void unregisterAcceptors(List<? extends Acceptor<?>> previousAcceptors) {
        if (!previousAcceptors.isEmpty()) {
            synchronized (this) {
                previousAcceptors.forEach(acceptor -> {
                    Class<? extends Acceptor<?>> acceptorType = resolve(acceptor.getClass());
                    if (acceptorType != null) {
                        acceptors.computeIfPresent(acceptorType, (k, v) -> {
                            var copy = new ArrayList<>(v);
                            copy.remove(acceptor);
                            acceptor.clear();
                            if (!copy.isEmpty()) {
                                copy.sort(null);
                                return Collections.unmodifiableList(copy);
                            } else {
                                return null;
                            }
                        });
                    }
                });
            }
        }
    }

    private record ReactableAcceptors(ReactorHandler handler, List<Acceptor<?>> acceptors) {}
}
