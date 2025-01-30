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

import { getIntervalFromDuration, Duration, maxInterval } from './intervalFromTimeRanges';

describe('intervalFromTimeRanges', () => {
  it('should calculate milliseconds', () => {
    expect(Duration.ofSeconds(1)).toEqual(1000);
    expect(Duration.ofMinutes(1)).toEqual(1000 * 60);
    expect(Duration.ofHours(1)).toEqual(1000 * 60 * 60);
    expect(Duration.ofDays(1)).toEqual(1000 * 60 * 60 * 24);
  });

  it('should return correct interval', () => {
    expect(getIntervalFromDuration(1)).toEqual(1000);
    expect(getIntervalFromDuration(Duration.ofMinutes(1))).toEqual(1000);
    expect(getIntervalFromDuration(Duration.ofMinutes(3))).toEqual(10000);
    expect(getIntervalFromDuration(Duration.ofDays(90))).toEqual(maxInterval);
  });
});
