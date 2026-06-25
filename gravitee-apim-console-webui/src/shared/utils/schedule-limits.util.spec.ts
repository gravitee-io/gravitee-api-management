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
import { FormControl } from '@angular/forms';

import { applyMinimumIntervalError, formatScheduleInterval, getMinimumIntervalError, getMinimumIntervalHint } from './schedule-limits.util';

describe('Schedule limits utils', () => {
  it.each([
    [300_000, '5 minutes'],
    [3_600_000, '1 hour'],
    [1_500, '1500 milliseconds'],
  ])('should format %s milliseconds as %s', (milliseconds, expected) => {
    expect(formatScheduleInterval(milliseconds)).toEqual(expected);
  });

  it('should map the minimum interval technical code to an inline error', () => {
    expect(
      getMinimumIntervalError({
        technicalCode: 'schedule.minimumIntervalExceeded',
        parameters: { minimumInterval: '300000' },
      }),
    ).toEqual('Schedule must not run more frequently than every 5 minutes');
  });

  it('should ignore unrelated errors', () => {
    expect(getMinimumIntervalError({ technicalCode: 'other' })).toBeNull();
  });

  it('should expose a formatted hint for an enabled limit', () => {
    expect(getMinimumIntervalHint(300_000)).toEqual('Minimum interval configured by your administrator: 5 minutes');
  });

  it('should apply a minimum interval error without replacing existing control errors', () => {
    const control = new FormControl();
    control.setErrors({ required: true });

    expect(
      applyMinimumIntervalError({ technicalCode: 'schedule.minimumIntervalExceeded', parameters: { minimumInterval: '300000' } }, control),
    ).toBe(true);
    expect(control.errors).toEqual({ required: true, minimumInterval: 'Schedule must not run more frequently than every 5 minutes' });
  });
});
