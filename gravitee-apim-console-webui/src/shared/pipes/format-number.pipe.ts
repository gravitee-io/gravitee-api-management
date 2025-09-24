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
   * Transforms a number into a human-readable string.
   * - Shows up to 2 decimal places, but omits them if they are zero.
   * - Adds suffixes (K, M, B) for large numbers.
   * @param value The number to format.
   * @returns A formatted string.
   */

  transform(value: number | null | undefined): string {
    // Handle invalid or null inputs
    if (value === null || value === undefined || isNaN(value)) {
      return 'N/A';
    }

    if (value === 0) {
      return '0';
    }

    if (value < 1) {
      return `< 1`;
    }

    // For numbers less than 1000, format with up to 2 decimals places
    if (Math.abs(value) < 1000) {
      const formatted = value.toLocaleString(undefined, { maximumFractionDigits: 2 });
      // Remove trailing .00 for whole numbers
      return formatted.endsWith('.00') ? formatted.slice(0, -3) : formatted;
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
        // Format to two decimals place
        const formattedValue = counter.toFixed(2);

        // Remove trailing '.00' or '.0' for whole numbers for a cleaner look
        let displayValue = formattedValue;
        if (formattedValue.endsWith('.00')) {
          displayValue = formattedValue.slice(0, -3);
        } else if (formattedValue.endsWith('.0')) {
          displayValue = formattedValue.slice(0, -2);
        }

        return `${displayValue}${unitName}`;
      }
    }

    // Fallback for any case not caught above (though unlikely with current logic)
    return `${value}`;
  }
}
