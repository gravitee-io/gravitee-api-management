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
package io.gravitee.apim.distribution.kafka;

import io.gravitee.apim.distribution.kafka.KafkaLinkageCheck.Unresolved;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Unresolved references that were analysed and accepted, typically dead code in a third-party jar a
 * plugin bundles. Each line reads {@code <plugin artifactId> <reference>  # <why it is accepted>}.
 *
 * <p>Entries name the plugin without its version, so a plugin bump keeps them. An entry that no longer
 * matches while its plugin is in the distribution is stale and fails the check too, so a fixed break
 * leaves the list instead of hiding the next one.
 */
public class Baseline {

    private static final Pattern PLUGIN_ZIP = Pattern.compile("(.+?)-\\d+\\.\\d+.*\\.zip");

    public record Verdict(List<Unresolved> unexpected, List<String> stale) {}

    private record Entry(String plugin, String reference) {
        @Override
        public String toString() {
            return plugin + " " + reference;
        }
    }

    private final Set<Entry> entries;

    private Baseline(Set<Entry> entries) {
        this.entries = entries;
    }

    public static Baseline parse(List<String> lines) {
        Set<Entry> entries = new LinkedHashSet<>();
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int hash = trimmed.indexOf('#');
            if (hash < 0 || trimmed.substring(hash + 1).isBlank()) {
                throw new IllegalArgumentException("Baseline entry without a justification after '#': " + line);
            }
            String[] fields = trimmed.substring(0, hash).strip().split("\\s+");
            if (fields.length != 2) {
                throw new IllegalArgumentException("Baseline entry is not '<plugin artifactId> <reference>  # <justification>': " + line);
            }
            entries.add(new Entry(fields[0], fields[1]));
        }
        return new Baseline(entries);
    }

    public Verdict evaluate(List<Unresolved> unresolved, Set<String> pluginZips) {
        List<Unresolved> unexpected = new ArrayList<>();
        Set<Entry> matched = new HashSet<>();
        for (Unresolved u : unresolved) {
            Entry entry = new Entry(artifactId(u.plugin()), u.reference());
            if (entries.contains(entry)) {
                matched.add(entry);
            } else {
                unexpected.add(u);
            }
        }
        Set<String> shipped = pluginZips.stream().map(Baseline::artifactId).collect(Collectors.toSet());
        List<String> stale = entries
            .stream()
            .filter(e -> shipped.contains(e.plugin()) && !matched.contains(e))
            .map(Entry::toString)
            .toList();
        return new Verdict(unexpected, stale);
    }

    private static String artifactId(String pluginZip) {
        Matcher matcher = PLUGIN_ZIP.matcher(pluginZip);
        return matcher.matches() ? matcher.group(1) : pluginZip;
    }
}
