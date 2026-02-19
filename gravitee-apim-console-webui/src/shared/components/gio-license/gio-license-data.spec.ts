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

import { ApimFeature, stringFeature } from './gio-license-data';

describe('GIO license features', () => {
  it('should convert string to Feature', () => {
    expect(stringFeature({ title: 'EE' }, 'apim-custom-roles')).toEqual(ApimFeature.APIM_CUSTOM_ROLES);
  });

  it('should convert apim-api-products string to APIM_API_PRODUCTS Feature', () => {
    expect(stringFeature({ title: 'EE' }, 'apim-api-products')).toEqual(ApimFeature.APIM_API_PRODUCTS);
  });

  it('should throw error with unknown Feature string', () => {
    expect(() => stringFeature({ title: 'EE' }, 'unknown feature')).toThrow();
  });
});
