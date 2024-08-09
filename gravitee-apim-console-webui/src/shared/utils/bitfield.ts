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
 * verify if a bit is set in bit field
 * @param field the bit field (if string should be in decimal)
 * @param mask the bit given as mask
 */
export function fieldIsSet(field: number | string, mask: number): boolean {
  // eslint-disable-next-line no-bitwise
  return (toField(field) & mask) === mask;
}

/**
 * set a bit in bit field
 * @param field the bit field (if string should be in decimal)
 * @param mask the bit given as mask
 */
export function fieldSet(field: number | string, mask: number): number {
  // eslint-disable-next-line no-bitwise
  return toField(field) | mask;
}

/**
 * Unset a bit of bit field
 * @param field the bit field (if string should be in decimal)
 * @param mask the bit given as mask
 */
export function fieldUnSet(field: number | string, mask: number): number {
  // eslint-disable-next-line no-bitwise
  return toField(field) & (~mask >>> 0);
}

function toField(field: number | string): number {
  return typeof field === 'string' ? parseInt(field, 10) : field;
}
