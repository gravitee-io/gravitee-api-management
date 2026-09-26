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
import { mapValues, omit } from 'lodash';

// Audit properties are also the audit search's only queryable index, so some of them classify the
// change instead of naming an entity it was made against. Those are not targets.
const CLASSIFICATION_PROPERTIES = ['ENCRYPTED'];

export function toAuditTargets(properties: Record<string, string>, metadata: Record<string, unknown>): Record<string, string> {
  return mapValues(omit(properties, CLASSIFICATION_PROPERTIES), (value, key) => metadata[`${key}:${value}:name`] as string);
}
