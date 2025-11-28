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
/*
 *
 *  * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import { duration, Duration } from 'moment';

export class WindowedCountFormatError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'WindowedCountFormatError';
  }
}

/**
 * Represents a count of events within a time window.
 * Used for calculating rates and validating time-based thresholds.
 */
export class WindowedCount {
  count: number;
  window: Duration;

  /**
   * Creates a new WindowedCount instance.
   * @param count - The number of events in the window.
   * @param window - The duration of the time window.
   */
  constructor(count: number, window: Duration) {
    this.window = window;
    this.count = count;
  }

  /**
   * Calculates the rate of events per second.
   * @returns The number of events per second.
   */
  public rate(): number {
    return this.count / this.window.asSeconds();
  }

  /**
   * Encodes the Windowed Count into a parsable string
   */
  public encode(): string {
    return this.count + '/' + this.window.toISOString();
  }

  /**
   * Parses a string representation of windowed count in format "count/duration".
   * @param format - The string to parse in format "count/duration".
   * @returns A new WindowedCount instance.
   * @throws Error if the format is invalid.
   */
  public static parse(format: string): WindowedCount {
    const parts = format.split('/');
    if (parts.length !== 2) {
      throw new WindowedCountFormatError('Invalid format: must be in count/duration format');
    }
    const count = Number.parseInt(parts[0], 10);
    const window = duration(parts[1]);
    if (Number.isNaN(count) || !window.isValid() || window.asMilliseconds() < 1) {
      throw new WindowedCountFormatError('Invalid format: count must be a number and duration must be valid');
    }
    return new WindowedCount(count, window);
  }

  /**
   * Checks if the windowed count is valid.
   * @returns true if count is positive and window duration is greater than zero, false otherwise.
   */
  isValid(): boolean {
    return this.count > 0 && this.window.asSeconds() > 0;
  }
}
