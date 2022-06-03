/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.swagger.parser;

import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAIParser extends AbstractDescriptorParser<OAIDescriptor> {

    private final Logger logger = LoggerFactory.getLogger(OAIParser.class);

    static {
        System.setProperty(String.format("%s.trustAll", io.swagger.v3.parser.util.RemoteUrl.class.getName()), Boolean.TRUE.toString());
    }

    public OAIDescriptor parse(String content, ParseOptions options) {
        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult parseResult;

        String path = content;
        Path tmp = null;

        if (!isLocationUrl(content)) {
            // Swagger v1 supports only a URL to read swagger: create temporary file for Swagger parser
            try {
                tmp = createTempFile(content);
                path = tmp.toAbsolutePath().toString();
            } catch (RuntimeException e) {
                logger.warn("Unable to create temporary file, raw content will be passed to OAI parser: {}", e.getMessage());
            }
        }

        parseResult = parser.readLocation(path, null, options);

        if (tmp != null) {
            deleteTempFile(tmp);
        }

        /* Hack due to swagger v1 converting issue
         * See https://github.com/swagger-api/swagger-parser/issues/1451
         */
        if (parseResult.getMessages() != null) {
            final List<String> filteredMessages = parseResult
                .getMessages()
                .stream()
                .filter(message -> message != null && !message.matches("^attribute info.contact.*"))
                .collect(Collectors.toList());
            parseResult.setMessages(filteredMessages);
        }

        if (parseResult.getOpenAPI() == null) {
            throw new SwaggerDescriptorException("Malformed descriptor");
        }

        OAIDescriptor descriptor = new OAIDescriptor(parseResult.getOpenAPI());
        descriptor.setMessages(parseResult.getMessages());
        return descriptor;
    }

    @Override
    public OAIDescriptor parse(String content) {
        return parse(content, null);
    }

    private Path createTempFile(String content) {
        Path temp = createSecuredTempFile();

        try (FileWriter out = new FileWriter(temp.toFile()); BufferedWriter bw = new BufferedWriter(out)) {
            bw.write(content);
        } catch (IOException ioe) {
            throw new TechnicalManagementException("An error occurs while trying to create temp file", ioe);
        }

        return temp;
    }

    private Path createSecuredTempFile() {
        String fileName = "gio_swagger_" + UuidString.generateRandom() + "_" + System.currentTimeMillis();
        if (SystemUtils.IS_OS_UNIX) {
            return createPosixSecuredFile(fileName);
        } else {
            return createNonPosixSecuredFile(fileName);
        }
    }

    private Path createPosixSecuredFile(String fileName) {
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
        try {
            return Files.createTempFile(fileName, ".tmp", attr);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /*
     * Ignoring security-sensitive usage of publicly writable directories as:
     *   - file name is not predictable
     *   - secured permissions will not be set only if something goes wrong system wise in setNonPosixPermissions
     *
     * see https://sonarcloud.io/organizations/gravitee-io/rules?open=java%3AS5443&rule_key=java%3AS5443
     */
    @SuppressWarnings("java:S5443")
    private Path createNonPosixSecuredFile(String fileName) {
        try {
            Path path = Files.createTempFile(fileName, ".tmp");
            setNonPosixPermissions(path.toFile());
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void setNonPosixPermissions(File file) {
        if (!file.setReadable(true, true) || !file.setWritable(true, true) || !file.setExecutable(false)) {
            logger.warn("Unable to set permissions on file {}, using default permissions", file.getAbsolutePath());
        }
    }

    private void deleteTempFile(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            logger.warn("Unable to delete temporary file {}", path);
        }
    }
}
