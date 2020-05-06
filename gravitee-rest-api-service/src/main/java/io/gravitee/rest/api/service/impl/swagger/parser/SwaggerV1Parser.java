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

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.util.RemoteUrl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SwaggerV1Parser extends AbstractSwaggerParser<Swagger> {

    static {
        System.setProperty(String.format("%s.trustAll", RemoteUrl.class.getName()), Boolean.TRUE.toString());
    }

    @Override
    public Swagger parse(String content) {
        if (! isLocationUrl(content)) {
            // Swagger v1 supports only a URL to read swagger: create temporary file for Swagger parser
            File temp = null;

            try {
                temp = createTempFile(content);
                return new SwaggerCompatConverter().read(temp.getAbsolutePath());
            } catch (IOException ioe) {

            } finally {
                if (temp != null) {
                    temp.delete();
                }
            }
        } else {
            try {
                return new SwaggerCompatConverter().read(content);
            } catch (IOException ioe) {

            }
        }

        return null;
    }

    private File createTempFile(String content) {
        File temp = null;
        String fileName = "gio_swagger_" + System.currentTimeMillis();
        BufferedWriter bw = null;
        FileWriter out = null;
        try {
            temp = File.createTempFile(fileName, ".tmp");
            out = new FileWriter(temp);
            bw = new BufferedWriter(out);
            bw.write(content);
            bw.close();
        } catch (IOException ioe) {
            // Fallback to the new parser
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return temp;
    }
}
