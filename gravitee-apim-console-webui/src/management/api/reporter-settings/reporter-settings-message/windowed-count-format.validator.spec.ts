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
/*
 *
 *  * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import { AbstractControl } from '@angular/forms';

import { isWindowedCountValidFormat } from './windowed-count-format.validator';

describe('isWindowedCountValidFormat', () => {
  it('should return null if the control value is empty', () => {
    const control = { value: '' } as AbstractControl;
    expect(isWindowedCountValidFormat()(control)).toBeUndefined();
  });

  it('should return null if the control value is valid', () => {
    const control = { value: '1/PT1S' } as AbstractControl;
    expect(isWindowedCountValidFormat()(control)).toBeUndefined();
  });

  it('should return null if windowed count is valid', () => {
    const control = { value: '5/PT10S' } as AbstractControl;
    expect(isWindowedCountValidFormat()(control)).toBeUndefined();
  });

  it('should return { invalidFormat: true } if windowed count is invalid', () => {
    const control = { value: '0/PT10S' } as AbstractControl;
    expect(isWindowedCountValidFormat()(control)).toEqual({ invalidFormat: true });
  });

  it('should return { invalidFormat: true } if windowed count cannot be parsed', () => {
    const control = { value: 'invalid_format' } as AbstractControl;
    expect(isWindowedCountValidFormat()(control)).toEqual({ invalidFormat: true });
  });
});
