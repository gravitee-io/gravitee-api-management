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

import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatNumber',
})
export class FormatNumberPipe implements PipeTransform {
  /**
   * Transforms a large number into a human-readable string with suffixes
   * like Thousand, Million, Billion, and rounds to a specified decimal place.
   * @param value The number to format.
   * @returns A formatted string.
   */

  transform(value: number | null | undefined): string {
    // Handle invalid or null inputs
    if (value === null || value === undefined || isNaN(value)) {
      return 'N/A';
    }

    if (value < 1000) {
      return `${value}`;
    }

    // Time intervals in milliseconds
    const intervals = {
      B: 1_000_000_000,
      M: 1_000_000,
      K: 1_000,
    };

    // Find the most appropriate unit to display
    for (const [unitName, intervalValue] of Object.entries(intervals)) {
      const counter = value / intervalValue;

      if (counter >= 1) {
        // Format to one decimal place
        const formattedValue = counter.toFixed(1);

        // Remove trailing '.0' for whole numbers for a cleaner look
        const displayValue = formattedValue.endsWith('.0') ? formattedValue.slice(0, -2) : formattedValue;

        return `${displayValue}${unitName}`;
      }
    }

    // Fallback for any case not caught above (though unlikely with current logic)
    return `${value}`;
  }
}
