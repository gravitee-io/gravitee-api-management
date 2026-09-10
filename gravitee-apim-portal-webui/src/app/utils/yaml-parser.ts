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
import { CORE_SCHEMA, NOT_RESOLVED, defineScalarTag, load } from 'js-yaml';

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

// js-yaml 5 merged resolve and construct: a scalar tag either returns the built value
// or NOT_RESOLVED. `identify` selects the tag when dumping, mirroring the former `instanceOf`.
export const binaryTag = defineScalarTag<string>('tag:yaml.org,2002:binary', {
  resolve(source) {
    // Ensure data is a valid Base64 string
    if (typeof source !== 'string' || !isValidBase64(source)) return NOT_RESOLVED;
    return base64ToUtf8(source);
  },
  identify(data: any) {
    return data instanceof String;
  },
  represent(value: any) {
    return utf8ToBase64(String(value));
  },
});

// Create schema with binary support. js-yaml 5 made JSON_SCHEMA strictly YAML 1.2 JSON, which
// no longer reads TRUE, Null or 0x1A; CORE_SCHEMA is what js-yaml 4 called JSON_SCHEMA.
const CUSTOM_SCHEMA = CORE_SCHEMA.withTags(binaryTag);

export function readYaml(content: string): any {
  return load(content, { schema: CUSTOM_SCHEMA });
}
