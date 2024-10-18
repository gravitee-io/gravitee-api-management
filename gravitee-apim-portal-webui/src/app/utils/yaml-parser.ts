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
import * as jsYAML from 'js-yaml';

import { Buffer } from 'buffer';

const binaryType = new jsYAML.Type('tag:yaml.org,2002:binary', {
  kind: 'scalar',
  resolve: (data: any) => {
    // Validate that the data is valid Base64
    return typeof data === 'string' && /^[A-Za-z0-9+/=]*$/.test(data);
  },
  construct: (data: string) => {
    // Convert Base64 to a Buffer
    return Buffer.from(data, 'base64').toString('utf-8'); // Convert to UTF-8 string
  },
  instanceOf: String,
  represent: (value: any) => {
    // Encode the value as Base64 for YAML representation
    return Buffer.from(String(value), 'utf-8').toString('base64');
  },
});

const schema = jsYAML.JSON_SCHEMA.extend([binaryType]);

export function readYaml(content: string): any {
  return jsYAML.load(content, { schema });
}
