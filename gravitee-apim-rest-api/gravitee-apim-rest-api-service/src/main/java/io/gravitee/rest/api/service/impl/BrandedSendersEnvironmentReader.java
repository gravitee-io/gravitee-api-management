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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.BrandedSenderConfig;
import io.gravitee.rest.api.model.settings.BrandedSenders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * Reconstructs the {@code email.branded_senders} parameter value from the native yaml list of objects an
 * on-prem operator writes in {@code gravitee.yml}:
 *
 * <pre>
 * email:
 *   branded_senders:
 *     - domains: [example.com]
 *       from: noreply@example.com
 *       subject: "[Example] %s"
 * </pre>
 *
 * <p>Spring flattens such a list into indexed properties ({@code email.branded_senders[0].from},
 * {@code email.branded_senders[0].domains[0]}, …), so {@code environment.getProperty("email.branded_senders")}
 * returns {@code null} and the value cannot be read as a single string. This reader walks the indexed
 * properties into a typed {@link BrandedSenderConfig} list and serialises it with {@link BrandedSenders#write(List)},
 * yielding the same JSON string form the console persists — including the {@code ';'} escaping the parameter
 * read path relies on and the size guard.</p>
 *
 * <p>The equivalent flat JSON string / env-var form ({@code gravitee_email_branded_senders=[{…}]}) is accepted
 * too and routed through the same {@link BrandedSenders#parse(String)} + {@link BrandedSenders#write(List)}
 * normalisation, so both forms get the same escaping, CR/LF validation and size guard and behave identically.
 * It is used by {@code ParameterServiceImpl} to seed the value at SYSTEM scope.</p>
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class BrandedSendersEnvironmentReader {

    private static final String PREFIX = Key.EMAIL_BRANDED_SENDERS.key();

    private final ConfigurableEnvironment environment;

    @Autowired
    public BrandedSendersEnvironmentReader(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Reads the branded-sender configurations, accepting either the native yaml list or the flat JSON string /
     * env-var form, and normalising both through {@link BrandedSenders#write(List)} so they share the same
     * {@code ';'}-escaping, CR/LF validation and size guard. Returns {@link Optional#empty()} when none are
     * declared (so the caller falls through to the env / org / default value), or when the declared value is
     * invalid or oversized — a broken on-prem config is logged and ignored rather than breaking the whole
     * settings response, and (crucially) leaves the console field editable rather than locked on a value that
     * is not actually in effect.
     */
    public Optional<String> read() {
        List<BrandedSenderConfig> configurations = readConfigurations();
        if (configurations.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(BrandedSenders.write(configurations));
        } catch (IllegalArgumentException e) {
            // write() signals invalid/oversized operator config only via IllegalArgumentException; catching that
            // (not RuntimeException) so an unrelated future bug surfaces instead of being mislabelled as bad config.
            // Source-neutral: read() serves both the native yaml list and the flat env-var / JSON form.
            log.error("Ignoring invalid '{}' branded-senders configuration", PREFIX, e);
            return Optional.empty();
        }
    }

    /**
     * Whether a <em>valid</em> branded-sender configuration is declared (in either storage form). Used to mark the
     * console field read-only, so an operator who seeds the value in {@code gravitee.yml} sees the same
     * "system-controlled" lock the flat form already gets — but only when the value is actually applied. A present
     * but invalid config yields {@code false} here (matching {@link #read()}), so the field stays editable and the
     * problem is logged rather than silently locked on the default.
     */
    public boolean isConfigured() {
        return read().isPresent();
    }

    private List<BrandedSenderConfig> readConfigurations() {
        // Flat JSON string / env-var form (e.g. gravitee_email_branded_senders): parse it here so read()'s
        // write() gives it the identical ';'-escaping, CR/LF validation and size guard as the native yaml list —
        // the two accepted forms then behave the same.
        String flat = environment.getProperty(PREFIX);
        if (flat != null && !flat.isBlank()) {
            return BrandedSenders.parse(flat);
        }

        // Native yaml list, flattened by Spring into indexed properties.
        List<BrandedSenderConfig> configurations = new ArrayList<>();
        int index = 0;
        while (hasEntry(index)) {
            String entryPrefix = PREFIX + "[" + index + "]";
            List<String> domains = readList(entryPrefix + ".domains");
            if (domains.isEmpty()) {
                log.warn("Branding entry '{}[{}]' has no 'domains'; it will never match a recipient", PREFIX, index);
            }
            configurations.add(
                BrandedSenderConfig.builder()
                    .domains(domains.isEmpty() ? null : domains)
                    .from(environment.getProperty(entryPrefix + ".from"))
                    .subject(environment.getProperty(entryPrefix + ".subject"))
                    .build()
            );
            index++;
        }
        return configurations;
    }

    /**
     * Whether an entry exists at {@code index}. Checks every field (not just {@code from} / {@code domains}) so an
     * incomplete middle entry does not prematurely terminate the loop and silently drop the valid entries after it;
     * a truly empty index ends the list.
     */
    private boolean hasEntry(int index) {
        String entryPrefix = PREFIX + "[" + index + "]";
        return (
            environment.getProperty(entryPrefix + ".from") != null ||
            environment.getProperty(entryPrefix + ".subject") != null ||
            environment.getProperty(entryPrefix + ".domains") != null ||
            environment.getProperty(entryPrefix + ".domains[0]") != null
        );
    }

    private List<String> readList(String listKey) {
        List<String> values = new ArrayList<>();
        int index = 0;
        String value;
        while ((value = environment.getProperty(listKey + "[" + index + "]")) != null) {
            values.add(value);
            index++;
        }
        if (values.isEmpty()) {
            // A scalar `domains: example.com` (rather than a list) binds to the bare key, not to domains[0].
            String scalar = environment.getProperty(listKey);
            if (scalar != null && !scalar.isBlank()) {
                values.add(scalar);
            }
        }
        return values;
    }
}
