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
package io.gravitee.apim.reporter.elasticsearch.mapping.serverless;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.reporter.elasticsearch.config.LifecycleGroup;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ServerlessRetention;
import io.gravitee.apim.reporter.elasticsearch.mapping.AbstractIndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.exception.ElasticsearchException;
import io.gravitee.elasticsearch.utils.Type;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.functions.Function;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.CustomLog;

/**
 * Renders the data stream templates of Elastic Cloud Serverless. Published in the es-index-mappings distribution without a
 * retention, as Serverless then keeps the documents forever; the reporter adds the retention of each type.
 * <p>
 * The templates the reporter writes carry a {@code _meta} naming the APIM version that wrote them: the reporter never
 * overwrites a template a newer APIM wrote, and never changes one it did not write. The retention of an existing data stream
 * follows the configured one, but a shorter one is only applied when the operator allows it, as it deletes documents.
 * <p>
 * The setup is retried until Serverless accepts it, so that a project created or granted its privileges after the gateway
 * started is written to without a restart.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class ServerlessIndexPreparer extends AbstractIndexPreparer {

    static final String MANAGED_BY = "gravitee-apim";

    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final String TEMPLATE_PRIVILEGE = "cluster privilege 'manage_index_templates'";
    private static final String LIFECYCLE_PRIVILEGE = "index privilege 'manage_data_stream_lifecycle'";
    private static final String UNLIMITED = "unlimited";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ServerlessRetention retention;
    private final String apimVersion;

    /**
     * The templates, and the retention of their data streams, already settled: a setup retry runs every type again, and would
     * otherwise log their error or information again.
     */
    private final Set<String> settledTemplates = ConcurrentHashMap.newKeySet();

    public ServerlessIndexPreparer(
        final ReporterConfiguration configuration,
        final PipelineConfiguration pipelineConfiguration,
        final FreeMarkerComponent freeMarkerComponent,
        final Client client
    ) {
        this(configuration, pipelineConfiguration, freeMarkerComponent, client, null, null);
    }

    /**
     * @param retention the retention of each type, or null for the templates of the es-index-mappings distribution
     * @param apimVersion the APIM version the {@code _meta} of the templates names
     */
    public ServerlessIndexPreparer(
        final ReporterConfiguration configuration,
        final PipelineConfiguration pipelineConfiguration,
        final FreeMarkerComponent freeMarkerComponent,
        final Client client,
        final ServerlessRetention retention,
        final String apimVersion
    ) {
        super(configuration, pipelineConfiguration, freeMarkerComponent, client, "/serverless");
        this.retention = retention;
        this.apimVersion = apimVersion;
    }

    @Override
    public String generateIndexTemplate(Type type) {
        final String template = super.generateIndexTemplate(type);
        if (retention == null) {
            return template;
        }
        try {
            final ObjectNode body = (ObjectNode) JSON.readTree(template);
            body.withObjectProperty("template").withObjectProperty("lifecycle").put("data_retention", retention.retentionOf(type));
            body
                .withObjectProperty("_meta")
                .put("managed_by", MANAGED_BY)
                .put("apim_version", apimVersion)
                .put("data_retention", retention.retentionOf(type));
            return JSON.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The serverless template of type [" + type.getType() + "] is not valid JSON", e);
        }
    }

    @Override
    protected Function<Type, CompletableSource> indexTypeMapper() {
        if (retention == null) {
            return super.indexTypeMapper();
        }
        return type -> {
            final String name = configuration.getIndexName() + '-' + type.getType();
            if (settledTemplates.contains(name)) {
                return Completable.complete();
            }
            return client
                .getIndexTemplate(name)
                .onErrorResumeNext(error ->
                    Maybe.error(new ServerlessSetupException("the index template [" + name + "]", TEMPLATE_PRIVILEGE, error))
                )
                .map(existing -> versionThatWrote(name, existing))
                .filter(version -> ApimVersion.isNewer(version, apimVersion))
                .doOnSuccess(version ->
                    log.info(
                        "The index template [{}] was written by APIM {}, newer than this gateway ({}): it is kept, along with the retention of its data stream",
                        name,
                        version,
                        apimVersion
                    )
                )
                .isEmpty()
                .flatMapCompletable(noNewerTemplate -> {
                    if (!noNewerTemplate) {
                        return Completable.complete();
                    }
                    return putTemplate(type, name).andThen(alignRetention(type, name));
                })
                .doOnComplete(() -> settledTemplates.add(name));
        };
    }

    private static String versionThatWrote(String name, JsonNode template) {
        final JsonNode meta = template.path("_meta");
        if (!MANAGED_BY.equals(meta.path("managed_by").asText())) {
            throw new UnmanagedTemplateException(name);
        }
        return meta.path("apim_version").asText();
    }

    private Completable putTemplate(Type type, String name) {
        return Completable.defer(() -> client.putIndexTemplate(name, generateIndexTemplate(type))).onErrorResumeNext(error ->
            Completable.error(new ServerlessSetupException("the index template [" + name + "]", TEMPLATE_PRIVILEGE, error))
        );
    }

    /**
     * Elasticsearch copies the lifecycle of a template to a data stream when it creates the stream only: an existing stream
     * keeps its retention until it is changed on the stream itself.
     */
    private Completable alignRetention(Type type, String name) {
        final String wanted = retention.retentionOf(type);
        return client
            .getDataStreamLifecycle(name)
            .flatMapCompletable(lifecycle -> alignRetention(type, name, lifecycle.path("data_retention").asText(UNLIMITED), wanted))
            .onErrorResumeNext(error ->
                Completable.error(
                    new ServerlessSetupException("the retention of the data stream [" + name + "]", LIFECYCLE_PRIVILEGE, error)
                )
            );
    }

    private Completable alignRetention(Type type, String name, String current, String wanted) {
        final Duration wantedDuration = ServerlessRetention.duration(wanted);
        final Optional<Duration> currentDuration = duration(current);
        if (currentDuration.filter(wantedDuration::equals).isPresent()) {
            return Completable.complete();
        }
        final boolean longer = currentDuration.filter(duration -> wantedDuration.compareTo(duration) > 0).isPresent();
        if (!longer && !retention.allowDecrease()) {
            log.error(
                "The data stream [{}] keeps its retention of {}: '{}' is set to {}, which is shorter. Set '{}' to true to apply it, which deletes the documents older than {}.",
                name,
                current,
                LifecycleGroup.of(type).retentionSetting(),
                wanted,
                ServerlessRetention.ALLOW_DECREASE_SETTING,
                wanted
            );
            return Completable.complete();
        }
        return client
            .putDataStreamLifecycle(name, JSON.createObjectNode().put("data_retention", wanted).toString())
            .doOnComplete(() -> log.info("The retention of the data stream [{}] changed from {} to {}", name, current, wanted));
    }

    /**
     * @return the duration of a retention Elasticsearch returns, or empty for an unlimited one or one this reporter cannot read
     */
    private static Optional<Duration> duration(String retention) {
        try {
            return Optional.of(ServerlessRetention.duration(retention));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public Completable prepare() {
        final AtomicInteger failedAttempts = new AtomicInteger();
        return super
            .prepare()
            .retryWhen(errors ->
                errors.flatMap(error -> {
                    if (error instanceof UnmanagedTemplateException) {
                        return Flowable.error(error);
                    }
                    final Duration delay = SetupBackoff.delay(failedAttempts.incrementAndGet());
                    log.warn(
                        "Unable to set up the Elastic Cloud Serverless data streams, retrying in {} s; documents are dropped until then. Cause: {}",
                        delay.toSeconds(),
                        describe(error)
                    );
                    return Flowable.timer(delay.toMillis(), TimeUnit.MILLISECONDS);
                })
            );
    }

    private static String describe(Throwable error) {
        final Integer status = error.getCause() instanceof ElasticsearchException cause ? cause.getStatusCode() : null;
        if (Integer.valueOf(FORBIDDEN).equals(status)) {
            final String privilege = error instanceof ServerlessSetupException setup ? setup.privilege : TEMPLATE_PRIVILEGE;
            return error.getMessage() + " (the API key lacks the " + privilege + ")";
        }
        if (Integer.valueOf(UNAUTHORIZED).equals(status)) {
            return error.getMessage() + " (Elasticsearch rejects the credentials)";
        }
        return error.getMessage();
    }

    private static class ServerlessSetupException extends RuntimeException {

        private final String privilege;

        ServerlessSetupException(String resource, String privilege, Throwable cause) {
            super("Unable to set up " + resource + ": " + cause.getMessage(), cause);
            this.privilege = privilege;
        }
    }
}
