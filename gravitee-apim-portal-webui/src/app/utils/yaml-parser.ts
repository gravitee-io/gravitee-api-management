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

function base64ToUtf8(base64: string): string {
  const binaryString = atob(base64);
  const bytes = Uint8Array.from(binaryString, char => char.charCodeAt(0));
  return new TextDecoder('utf-8').decode(bytes);
}

function utf8ToBase64(str: string): string {
  const bytes = new TextEncoder().encode(str);
  const binaryString = Array.from(bytes, byte => String.fromCharCode(byte)).join('');
  return btoa(binaryString);
}

function isValidBase64(data: string): boolean {
  try {
    atob(data);
    return true;
  } catch {
    return false;
  }
}

const binaryType = new jsYAML.Type('tag:yaml.org,2002:binary', {
  kind: 'scalar',
  resolve(data: any) {
    // Ensure data is a valid Base64 string
    if (typeof data !== 'string') return false;
    return isValidBase64(data);
  },
  construct(data: string) {
    return base64ToUtf8(data);
  },
  instanceOf: String,
  represent(value: any) {
    return utf8ToBase64(String(value));
  },
});

// Create schema with binary support
const CUSTOM_SCHEMA = jsYAML.JSON_SCHEMA.extend([binaryType]);

export function readYaml(content: string): any {
  return jsYAML.load(content, { schema: CUSTOM_SCHEMA });
}
