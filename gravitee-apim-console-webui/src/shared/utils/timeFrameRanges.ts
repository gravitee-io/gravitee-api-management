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
import moment from 'moment/moment';

export interface TimeRangeParams {
  id?: string;
  from: number;
  to: number;
  interval?: number;
}

export const timeInMilliseconds = {
  '1m': 1000 * 60,
  '1h': 1000 * 60 * 60,
  '1d': 1000 * 60 * 60 * 24,
  '1w': 1000 * 60 * 60 * 24 * 7,
  '1M': 1000 * 60 * 60 * 24 * 30,
};

export const timeFrameRangesParams = (id: string, nbValuesByBucket = 30): TimeRangeParams => {
  const nowLocal = moment().valueOf();
  return {
    id,
    from: nowLocal - timeInMilliseconds[id],
    to: nowLocal,
    interval: timeInMilliseconds[id] / nbValuesByBucket,
  };
};

const getTimeFramesRangesParams = function () {
  return timeFrameRangesParams(this.id);
};

export const timeFrames = [
  {
    label: 'Last minute',
    id: '1m',
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
