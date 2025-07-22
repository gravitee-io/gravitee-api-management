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
  resolve(data: any) {
    // Ensure data is a valid Base64 string
    if (typeof data !== 'string') return false;
    try {
      Buffer.from(data, 'base64');
      return true;
    } catch {
      return false;
    }
  },
  construct(data: string) {
    return Buffer.from(data, 'base64').toString('utf-8');
  },
  instanceOf: String,
  represent(value: any) {
    return Buffer.from(String(value), 'utf-8').toString('base64');
  },
});

// Create schema with binary support
const CUSTOM_SCHEMA = jsYAML.JSON_SCHEMA.extend([binaryType]);

export function readYaml(content: string): any {
  return jsYAML.load(content, { schema: CUSTOM_SCHEMA });
}
