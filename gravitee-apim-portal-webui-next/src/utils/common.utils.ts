/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
 * Compares two arrays for equality by checking each element at the same index.
 * Returns true if arrays are equal, false otherwise.
 * Handles null and undefined cases.
 */
export function arrayEquals<T>(a: T[] | undefined | null, b: T[] | undefined | null): boolean {
  if (a === b) return true;
  if (!a || !b) return a === b;
  if (a.length !== b.length) return false;
  return a.every((val, index) => val === b[index]);
}

/**
 * Parses a query parameter that can be a string, array, or undefined into a string array.
 */
export function parseArrayParam(param: unknown): string[] {
  if (!param) return [];
  if (Array.isArray(param)) {
    return param.filter((v): v is string => typeof v === 'string');
  }
  return typeof param === 'string' ? [param] : [];
}

/**
 * Parses a query parameter into a valid page number.
 * Returns defaultPage if the parameter is invalid.
 */
export function parsePageParam(param: unknown, defaultPage: number = 1): number {
  const num = Number(param);
  return Number.isFinite(num) && num > 0 ? num : defaultPage;
}

/**
 * Parses a query parameter into a valid page size.
 * Returns defaultSize if the parameter is invalid or exceeds maxSize.
 */
export function parseSizeParam(param: unknown, defaultSize: number = 10, maxSize: number = 100): number {
  const num = Number(param);
  return Number.isFinite(num) && num > 0 && num <= maxSize ? num : defaultSize;
}

/**
 * Converts a string to title case (first letter uppercase, rest lowercase).
 */
export function toTitleCase(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}

/**
 * Compares two filter objects for equality.
 * Compares arrays and primitive values within the filter objects.
 */
export function areFiltersEqual<T extends Record<string, unknown>>(prev: T, curr: T): boolean {
  const keys = Object.keys(prev) as (keyof T)[];
  return keys.every(key => {
    const prevValue = prev[key];
    const currValue = curr[key];

    if (Array.isArray(prevValue) && Array.isArray(currValue)) {
      return arrayEquals(prevValue, currValue);
    }

    return prevValue === currValue;
  });
}
