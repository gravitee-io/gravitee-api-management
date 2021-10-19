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
import { ITimeframe, TimeframeRanges } from './quick-time-range.component';

class QuickTimeRangeController {
  private timeframe: ITimeframe;
  private timeframes: ITimeframe[];
  private onTimeframeChange: any;

  constructor() {
    'ngInject';

    this.timeframes = [
      TimeframeRanges.LAST_MINUTE,
      TimeframeRanges.LAST_HOUR,
      TimeframeRanges.LAST_DAY,
      TimeframeRanges.LAST_WEEK,
      TimeframeRanges.LAST_MONTH,
    ];

    if (!this.timeframe) {
      this.timeframe = TimeframeRanges.LAST_MINUTE;
    }
  }

  updateTimeframe() {
    this.onTimeframeChange({ timeframe: this.timeframe });
  }
}

export default QuickTimeRangeController;
