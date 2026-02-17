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
package io.gravitee.apim.distribution.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.reporter.elasticsearch.ElasticsearchReporter;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.AbstractIndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.utils.Type;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.reflections.Reflections;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ESIndexMappingsDistributionBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Generates ES pipeline and index mapping for each implementation version supported by the elastic-reporter (es8x, es9x, opensearch, ...).
     * This main method is supposed to be invoked by Maven when building a new version of APIM.
     *
     * @param args target directory to output the generated templates.
     * @throws Exception any exception that occurs during the generation process.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            args = new String[] { "es-index-templates" };
        }

        FreeMarkerComponent freeMarkerComponent = FreeMarkerComponent.builder()
            .classLoader(ElasticsearchReporter.class.getClassLoader())
            .classLoaderTemplateBase("/freemarker/")
            .build();

        ReporterConfiguration configuration = new ReporterConfiguration();
        PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(freeMarkerComponent);

        Reflections reflections = new Reflections("io.gravitee.apim.reporter.elasticsearch");
        Set<Class<? extends AbstractIndexPreparer>> classes = reflections.getSubTypesOf(AbstractIndexPreparer.class);

        for (Class<? extends AbstractIndexPreparer> clazz : classes) {
            log.info("Found index preparer class: {}", clazz.getName());
            AbstractIndexPreparer indexPreparer = buildIndexPreparer(clazz, configuration, pipelineConfiguration, freeMarkerComponent);

            // Create the target directory structure if it does not exist.
            Path dir = ensureDirectoryStructure(args[0], indexPreparer);

            // Write the pipeline file.
            Path file = dir.resolve("pipeline.json");
            Files.writeString(file, pipelineConfiguration.createPipeline());
            log.info("Generated pipeline template {}", indexPreparer.getTemplatePathPrefix() + "/" + file.getFileName());

            for (Type type : Type.values()) {
                String template = jsonPrettify(indexPreparer.generateIndexTemplate(type));
                log.debug("{}/{}.json: \n{}", indexPreparer.getTemplatePathPrefix(), type.getType(), template);

                // Write the index mapping file.
                file = dir.resolve(type.getType() + ".json");
                Files.writeString(file, template);

                log.info("Generated index template {}", indexPreparer.getTemplatePathPrefix() + "/" + file.getFileName());
            }
        }
    }

    private static AbstractIndexPreparer buildIndexPreparer(
        Class<? extends AbstractIndexPreparer> clazz,
        ReporterConfiguration configuration,
        PipelineConfiguration pipelineConfiguration,
        FreeMarkerComponent freeMarkerComponent
    ) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<? extends AbstractIndexPreparer> constructor = clazz.getConstructor(
            ReporterConfiguration.class,
            PipelineConfiguration.class,
            FreeMarkerComponent.class,
            Client.class
        );
        AbstractIndexPreparer indexPreparer = constructor.newInstance(configuration, pipelineConfiguration, freeMarkerComponent, null);
        return indexPreparer;
    }

    private static Path ensureDirectoryStructure(String target, AbstractIndexPreparer indexPreparer) throws IOException {
        String folderPath = target + "/" + indexPreparer.getTemplatePathPrefix();
        Path dir = java.nio.file.Paths.get(folderPath);
        if (!dir.toFile().exists()) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    @SneakyThrows
    private static String jsonPrettify(String jsonString) {
        JsonNode json = mapper.readTree(jsonString);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }
}
