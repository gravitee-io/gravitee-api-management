/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import moment from 'moment';

export interface TimeRangeParams {
  id?: string;
  from: number;
  to: number;
  interval?: number;
}

const MINUTE_MS = 1000 * 60;
const HOUR_MS = MINUTE_MS * 60;
const DAY_MS = HOUR_MS * 24;

export const timeInMilliseconds: Record<BasicTimeframe, number> = {
  '1m': MINUTE_MS,
  '5m': MINUTE_MS * 5,
  '1h': HOUR_MS,
  '1d': DAY_MS,
  '1w': DAY_MS * 7,
  '1M': DAY_MS * 30,
};

export type BasicTimeframe = '1m' | '5m' | '1h' | '1d' | '1w' | '1M';

export const timeFrameRangesParams = (id: BasicTimeframe, nbValuesByBucket = 30): TimeRangeParams => {
  const nowLocal = moment().valueOf();
  return {
    id,
    from: nowLocal - timeInMilliseconds[id],
    to: nowLocal,
    interval: Math.floor(timeInMilliseconds[id] / nbValuesByBucket),
  };
};

const getTimeFramesRangesParams = function (this: { id: BasicTimeframe }) {
  return timeFrameRangesParams(this.id);
};

export const timeFrames: { label: string; id: BasicTimeframe; timeFrameRangesParams: typeof getTimeFramesRangesParams }[] = [
  {
    label: 'Last minute',
    id: '1m',
    timeFrameRangesParams: getTimeFramesRangesParams,
  },
  {
    label: 'Last 5 minutes',
    id: '5m',
    timeFrameRangesParams: getTimeFramesRangesParams,
  },
  {
    label: 'Last hour',
    id: '1h',
    timeFrameRangesParams: getTimeFramesRangesParams,
  },
  {
    label: 'Last day',
    id: '1d',
    timeFrameRangesParams: getTimeFramesRangesParams,
  },
  {
    label: 'Last week',
    id: '1w',
    timeFrameRangesParams: getTimeFramesRangesParams,
  },
  {
    label: 'Last month',
    id: '1M',
    timeFrameRangesParams: getTimeFramesRangesParams,
  },
];
export const customTimeFrames = [
  {
    label: 'Custom',
    id: 'custom',
  },
];

export const DATE_TIME_FORMATS = {
  parseInput: 'Y-M-DD HH:mm:ss',
  fullPickerInput: 'Y-M-DD HH:mm:ss',
  datePickerInput: 'Y-M-D',
  timePickerInput: 'HH:mm:ss',
  monthYearLabel: 'MMM y',
  dateA11yLabel: 'Y-M-D',
  monthYearA11yLabel: 'Y MMMM',
};

export const calculateCustomInterval = (from: number, to: number, nbValuesByBucket = 30) => {
  const range: number = to - from;
  return Math.floor(range / nbValuesByBucket);
};
