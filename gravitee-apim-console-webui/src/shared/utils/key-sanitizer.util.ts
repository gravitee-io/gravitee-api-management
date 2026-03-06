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

/**
 * Normalizes user input to produce a valid tag key that meets backend requirements
 * (alphanumeric with hyphens only, no diacritics or special characters).
 */
export function sanitizeKeyBase(key: string): string {
  return key
    .normalize('NFD')
    .replaceAll(/[\u0300-\u036f]+/g, '')
    .toLowerCase()
    .replaceAll(/[^a-z\d\s-]/g, '')
    .trim()
    .replaceAll(/[^a-z\d]+/g, '-');
}

/**
 * Finalizes key sanitization by removing trailing hyphens.
 * This should be called on blur to ensure the key requirements.
 */
export function sanitizeKeyFinal(key: string): string {
  let sanitized = sanitizeKeyBase(key);
  while (sanitized.endsWith('-')) {
    sanitized = sanitized.slice(0, -1);
  }
  return sanitized;
}
