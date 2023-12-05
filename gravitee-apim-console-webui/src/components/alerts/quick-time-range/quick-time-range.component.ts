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

const nbValuesByBucket = 30;

export class TimeframeRanges {
  static readonly LAST_MINUTE: ITimeframe = {
    id: '1m',
    title: 'Last minute',
    range: 1000 * 60,
    interval: (1000 * 60) / nbValuesByBucket,
  };
  static readonly LAST_HOUR: ITimeframe = {
    id: '1h',
    title: 'Last hour',
    range: 1000 * 60 * 60,
    interval: (1000 * 60 * 60) / nbValuesByBucket,
  };
  static readonly LAST_DAY: ITimeframe = {
    id: '1d',
    title: 'Last day',
    range: 1000 * 60 * 60 * 24,
    interval: (1000 * 60 * 60 * 24) / nbValuesByBucket,
  };
  static readonly LAST_WEEK: ITimeframe = {
    id: '1w',
    title: 'Last week',
    range: 1000 * 60 * 60 * 24 * 7,
    interval: (1000 * 60 * 60 * 24 * 7) / nbValuesByBucket,
  };
  static readonly LAST_MONTH: ITimeframe = {
    id: '1M',
    title: 'Last month',
    range: 1000 * 60 * 60 * 24 * 30,
    interval: (1000 * 60 * 60 * 24 * 30) / nbValuesByBucket,
  };
}

export interface ITimeframe {
  id: string;
  title: string;
  range: number;
  interval: number;
}

const QuickTimeRangeComponent: ng.IComponentOptions = {
  template: require('./quick-time-range.html'),
  controller: 'QuickTimeRangeController',
  bindings: {
    onTimeframeChange: '&',
  },
};

export default QuickTimeRangeComponent;
