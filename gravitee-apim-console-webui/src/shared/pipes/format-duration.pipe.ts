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

const MS_IN_SECOND = 1000;
const MS_IN_MINUTE = MS_IN_SECOND * 60;
const MS_IN_HOUR = MS_IN_MINUTE * 60;
const MS_IN_DAY = MS_IN_HOUR * 24;
const MS_IN_MONTH = MS_IN_DAY * 30;

@Pipe({
  name: 'formatDuration',
})
export class FormatDurationPipe implements PipeTransform {
  private readonly intervals = {
    mo: MS_IN_MONTH,
    d: MS_IN_DAY,
    h: MS_IN_HOUR,
    min: MS_IN_MINUTE,
    s: MS_IN_SECOND,
  };

  /**
   * Transforms a number of milliseconds into a human-readable string
   * with decimal accuracy (e.g., "1.5 hours").
   * @param value The number of milliseconds.
   * @returns A formatted string.
   */
  transform(value: number | null | undefined): string {
    // Handle invalid or null inputs
    if (value === null || value === undefined || isNaN(value)) {
      return 'N/A';
    }

    if (value < 1000) {
      return `${value}ms`;
    }

    // Find the most appropriate unit to display
    for (const [unitName, intervalValue] of Object.entries(this.intervals)) {
      const counter = value / intervalValue;

      if (counter >= 1) {
        // Format to one decimal place
        const formattedValue = counter.toFixed(1);

        // Remove trailing '.0' for whole numbers for a cleaner look
        const displayValue = formattedValue.endsWith('.0') ? formattedValue.slice(0, -2) : formattedValue;

        return `${displayValue}${unitName}`;
      }
    }

    // // Fallback for any case not caught above (though unlikely with current logic)
    return `${value}ms`;
  }
}
