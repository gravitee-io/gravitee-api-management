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
// __mocks__/chartjs-adapter-date-fns.js
// Minimal mock of the date adapter so Chart.js time scale doesn't crash in tests.

const Chart = require('chart.js');

// In Chart.js v3/v4, adapters are under Chart._adapters._date
const adapters = Chart._adapters || Chart.adapters;

if (adapters && adapters._date && typeof adapters._date.override === 'function') {
  adapters._date.override({
    formats: () => ({}),

    parse: value => {
      if (value == null) {
        return null;
      }
      const date = value instanceof Date ? value : new Date(value);
      const time = date.getTime();
      return Number.isNaN(time) ? null : time;
    },

    format: (time /* timestamp */, formatString) => {
      const date = new Date(time);
      if (Number.isNaN(date.getTime())) {
        return '';
      }
      return date.toISOString();
    },

    add: (time, amount, unit) => {
      const base = new Date(time).getTime();
      if (Number.isNaN(base)) {
        return time;
      }

      const unitToMs = {
        millisecond: 1,
        second: 1000,
        minute: 60 * 1000,
        hour: 60 * 60 * 1000,
        day: 24 * 60 * 60 * 1000,
        week: 7 * 24 * 60 * 60 * 1000,
        month: 30 * 24 * 60 * 60 * 1000,
        quarter: 3 * 30 * 24 * 60 * 60 * 1000,
        year: 365 * 24 * 60 * 60 * 1000,
      };

      const ms = unitToMs[unit] ?? 1;
      return base + amount * ms;
    },

    diff: (max, min, unit) => {
      const unitToMs = {
        millisecond: 1,
        second: 1000,
        minute: 60 * 1000,
        hour: 60 * 60 * 1000,
        day: 24 * 60 * 60 * 1000,
        week: 7 * 24 * 60 * 60 * 1000,
        month: 30 * 24 * 60 * 60 * 1000,
        quarter: 3 * 30 * 24 * 60 * 60 * 1000,
        year: 365 * 24 * 60 * 60 * 1000,
      };

      const ms = unitToMs[unit] ?? 1;
      return (max - min) / ms;
    },

    startOf: (time, unit) => {
      const date = new Date(time);

      switch (unit) {
        case 'second':
          date.setMilliseconds(0);
          break;
        case 'minute':
          date.setSeconds(0, 0);
          break;
        case 'hour':
          date.setMinutes(0, 0, 0);
          break;
        case 'day':
          date.setHours(0, 0, 0, 0);
          break;
        case 'week':
          date.setDate(date.getDate() - date.getDay());
          date.setHours(0, 0, 0, 0);
          break;
        case 'month':
          date.setDate(1);
          date.setHours(0, 0, 0, 0);
          break;
        case 'quarter':
          date.setMonth(Math.floor(date.getMonth() / 3) * 3, 1);
          date.setHours(0, 0, 0, 0);
          break;
        case 'year':
          date.setMonth(0, 1);
          date.setHours(0, 0, 0, 0);
          break;
        default:
          break;
      }

      return date.getTime();
    },

    endOf: (time, unit) => {
      const date = new Date(time);

      switch (unit) {
        case 'second':
          date.setMilliseconds(999);
          break;
        case 'minute':
          date.setSeconds(59, 999);
          break;
        case 'hour':
          date.setMinutes(59, 59, 999);
          break;
        case 'day':
          date.setHours(23, 59, 59, 999);
          break;
        case 'week':
          date.setDate(date.getDate() - date.getDay() + 6);
          date.setHours(23, 59, 59, 999);
          break;
        case 'month':
          date.setMonth(date.getMonth() + 1, 0);
          date.setHours(23, 59, 59, 999);
          break;
        case 'quarter':
          date.setMonth(Math.floor(date.getMonth() / 3) * 3 + 3, 0);
          date.setHours(23, 59, 59, 999);
          break;
        case 'year':
          date.setMonth(11, 31);
          date.setHours(23, 59, 59, 999);
          break;
        default:
          break;
      }

      return date.getTime();
    },
  });
}

module.exports = {};
