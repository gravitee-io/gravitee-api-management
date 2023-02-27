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
import { TestBed } from '@angular/core/testing';

import { ICON_NAMESPACE, IconService } from './icon.service';

import { GioHttpTestingModule } from '../shared/testing';

describe('IconService', () => {
  let iconService: IconService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    iconService = TestBed.inject<IconService>(IconService);
  });

  describe('icon', () => {
    it('should register an svg', () => {
      let icon = iconService.registerSvg('foobar', 'data:image/svg+xml;base64,abcd');
      expect(icon).toEqual(`${ICON_NAMESPACE}:foobar`);

      icon = iconService.registerSvg('foobar', 'label');
      expect(icon).toEqual('label');
    });
  });
});
