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
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { isPlainObject, omit } from 'lodash';

// A schema using one of these keywords can declare properties outside `properties`: its form value is saved as is.
const KEYWORDS_DECLARING_OTHER_PROPERTIES: (keyof GioJsonSchema)[] = [
  '$ref',
  'allOf',
  'anyOf',
  'oneOf',
  'if',
  'then',
  'else',
  'dependencies',
  'additionalProperties',
  'patternProperties',
];

/**
 * A schema form only emits the properties its schema declares. Saving its value as the whole configuration would
 * delete properties written by other clients, such as the tools of an MCP Studio endpoint, so they are kept from the
 * stored configuration.
 */
export function keepUndeclaredConfiguration(
  schema: GioJsonSchema | null,
  storedConfiguration: unknown,
  formConfiguration: unknown,
): unknown {
  if (
    !schema ||
    KEYWORDS_DECLARING_OTHER_PROPERTIES.some(keyword => schema[keyword] !== undefined) ||
    !isPlainObject(storedConfiguration) ||
    !isPlainObject(formConfiguration)
  ) {
    return formConfiguration;
  }
  return { ...omit(storedConfiguration as object, Object.keys(schema.properties ?? {})), ...(formConfiguration as object) };
}
