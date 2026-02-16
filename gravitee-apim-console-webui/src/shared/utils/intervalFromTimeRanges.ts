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

export class Duration {
  static ofSeconds(seconds: number): number {
    return seconds * 1000;
  }

  static ofMinutes(minutes: number): number {
    return minutes * this.ofSeconds(60);
  }

  static ofHours(hours: number): number {
    return hours * this.ofMinutes(60);
  }

  static ofDays(days: number): number {
    return days * this.ofHours(24);
  }
}

export const maxInterval = Duration.ofHours(12);

const intervalsData = [
  [Duration.ofMinutes(1), Duration.ofSeconds(1)],
  [Duration.ofMinutes(5), Duration.ofSeconds(10)],
  [Duration.ofMinutes(30), Duration.ofSeconds(15)],
  [Duration.ofHours(1), Duration.ofSeconds(30)],
  [Duration.ofHours(3), Duration.ofMinutes(1)],
  [Duration.ofHours(6), Duration.ofMinutes(2)],
  [Duration.ofHours(12), Duration.ofMinutes(5)],
  [Duration.ofDays(1), Duration.ofMinutes(10)],
  [Duration.ofDays(3), Duration.ofMinutes(30)],
  [Duration.ofDays(7), Duration.ofHours(1)],
  [Duration.ofDays(14), Duration.ofHours(3)],
  [Duration.ofDays(30), Duration.ofHours(6)],
  [Duration.ofDays(90), maxInterval],
];

export function getIntervalFromDuration(duration: number) {
  const interval = intervalsData.find(interval => interval[0] >= duration);
  return interval ? interval[1] : maxInterval;
}

export function calculateCustomInterval(from: number, to: number): number {
  if (from > to) return;
  const duration = to - from;
  return getIntervalFromDuration(duration);
}
