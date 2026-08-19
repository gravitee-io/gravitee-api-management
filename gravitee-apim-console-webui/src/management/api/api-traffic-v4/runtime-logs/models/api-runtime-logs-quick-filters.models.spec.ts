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
import { DEFAULT_PERIOD, NONE_PERIOD, resolveInitialLogPeriod } from './api-runtime-logs-quick-filters.models';

describe('resolveInitialLogPeriod', () => {
  it('should keep an explicit period even when dates are present', () => {
    expect(resolveInitialLogPeriod(DEFAULT_PERIOD, 1, 2)).toEqual(DEFAULT_PERIOD);
  });

  it('should use None when there is no period but dates are present', () => {
    expect(resolveInitialLogPeriod(undefined, 1, undefined)).toEqual(NONE_PERIOD);
    expect(resolveInitialLogPeriod(undefined, undefined, 2)).toEqual(NONE_PERIOD);
  });

  it('should use Last 5 Minutes when there is no period and no dates', () => {
    expect(resolveInitialLogPeriod()).toEqual(DEFAULT_PERIOD);
    expect(resolveInitialLogPeriod(undefined, undefined, undefined)).toEqual(DEFAULT_PERIOD);
  });
});
