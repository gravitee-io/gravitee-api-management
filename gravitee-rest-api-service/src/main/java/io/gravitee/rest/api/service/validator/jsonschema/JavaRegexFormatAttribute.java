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
package io.gravitee.rest.api.service.validator.jsonschema;

import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JavaRegexFormatAttribute extends AbstractFormatAttribute {

    private static final FormatAttribute instance = new JavaRegexFormatAttribute();
    public static final String NAME = "java-regex";

    private JavaRegexFormatAttribute() {
        super(NAME, NodeType.STRING);
    }

    public static FormatAttribute getInstance() {
        return instance;
    }

    @Override
    public void validate(ProcessingReport report, MessageBundle bundle, FullData data) throws ProcessingException {

        final String input = data.getInstance().getNode().textValue();

        try {
            Pattern.compile(input);
        } catch (PatternSyntaxException pse) {

            ProcessingMessage processingMessage = data.newMessage().put("domain", "validation")
                    .put("keyword", "format")
                    .put("attribute", NAME)
                    .setMessage("Invalid java regular expression [" + input + "]")
                    .put("value", input);

            report.error(processingMessage);
        }
    }
}
