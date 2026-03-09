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
type DeepObject = { [key: string]: DeepValue };
type DeepValue = string | number | boolean | null | DeepObject | DeepArray;
type DeepArray = DeepValue[];

export const deepEqualIgnoreOrder = (a: unknown, b: unknown): boolean => {
  const sortKeys = (obj: DeepValue): DeepValue => {
    // If it's not an object or array, return the value as is
    if (typeof obj !== 'object' || obj === null) {
      return obj;
    }

    // If it's an array, sort the array elements
    if (Array.isArray(obj)) {
      return obj.map(sortKeys).sort((x, y) => {
        if (typeof x === 'string' && typeof y === 'string') {
          return x.localeCompare(y);
        }
        return 0;
      });
    }

    // If it's a plain object, sort its keys and recursively sort its values
    const sortedObj: DeepObject = {};
    Object.keys(obj)
      .sort((a, b) => a.localeCompare(b))
      .forEach(key => {
        sortedObj[key] = sortKeys((obj as DeepObject)[key]);
      });

    return sortedObj;
  };

  return JSON.stringify(sortKeys(a as DeepValue)) === JSON.stringify(sortKeys(b as DeepValue));
};
