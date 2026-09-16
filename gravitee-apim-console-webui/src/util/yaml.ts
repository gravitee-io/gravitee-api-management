/*
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
import { binaryTag, CORE_SCHEMA, loadAll, mergeTag, omapTag, pairsTag, setTag, timestampTag, YAMLException } from 'js-yaml';

/**
 * What js-yaml 4 called DEFAULT_SCHEMA: the YAML 1.2 core schema plus the 1.1 collection and
 * scalar tags. js-yaml 5 dropped that name and neither replacement is equivalent — its own
 * default, CORE_SCHEMA, throws on `!!binary`, and YAML11_SCHEMA reads `yes`, `no` and `on` as
 * booleans where OpenAPI specs expect the strings.
 */
export const YAML_SCHEMA = CORE_SCHEMA.withTags(timestampTag, mergeTag, binaryTag, omapTag, pairsTag, setTag);

/**
 * `load` as js-yaml 4 behaved: an input holding no document — empty, blank or comments only —
 * reads as `undefined` rather than throwing, and a multi-document stream is still refused.
 * js-yaml 5 throws in the first case, which callers here treat as invalid content.
 */
export function loadYaml(content: string): unknown {
  const documents = loadAll(content, { schema: YAML_SCHEMA });

  if (documents.length === 0) {
    return undefined;
  }
  if (documents.length > 1) {
    throw new YAMLException('expected a single document in the stream, but found more');
  }
  return documents[0];
}
