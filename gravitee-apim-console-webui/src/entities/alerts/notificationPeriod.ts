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
export interface NotificationPeriod {
  days: Array<number>;
  beginHour: number;
  endHour: number;
  zoneId: string;
}

export class Days {
  static readonly Monday = 1;
  static readonly Tuesday = 2;
  static readonly Wednesday = 3;
  static readonly Thursday = 4;
  static readonly Friday = 5;
  static readonly Saturday = 6;
  static readonly Sunday = 7;

  static readonly daysMap: Map<string, number> = new Map([
    ['Monday', Days.Monday],
    ['Tuesday', Days.Tuesday],
    ['Wednesday', Days.Wednesday],
    ['Thursday', Days.Thursday],
    ['Friday', Days.Friday],
    ['Saturday', Days.Saturday],
    ['Sunday', Days.Sunday],
  ]);

  static getBusinessDays(): string[] {
    return ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];
  }

  static getAllDayNames(): string[] {
    return Array.from(Days.daysMap.keys());
  }

  static dayToNumber(dayName: string): number | undefined {
    return Days.daysMap.get(dayName);
  }
}
