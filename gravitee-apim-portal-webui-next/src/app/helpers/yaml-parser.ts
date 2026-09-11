/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import * as jsYAML from 'js-yaml';

// CORE_SCHEMA is what js-yaml 4 called JSON_SCHEMA. The name JSON_SCHEMA still exists in 5 but
// now means strict YAML 1.2 JSON, which reads TRUE, Null and 0x1A back as plain strings.
// loadAll keeps the js-yaml 4 reading of an input with no document: undefined, where 5 throws.
export function readYaml(content: string): unknown {
  const documents = jsYAML.loadAll(content, { schema: jsYAML.CORE_SCHEMA });

  if (documents.length === 0) {
    return undefined;
  }
  if (documents.length > 1) {
    throw new jsYAML.YAMLException('expected a single document in the stream, but found more');
  }
  return documents[0];
}
