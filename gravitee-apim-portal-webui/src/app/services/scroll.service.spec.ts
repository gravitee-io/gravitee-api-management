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

import { ScrollService } from './scroll.service';

describe('ScrollServiceService', () => {
  let service: ScrollService;

  beforeEach(() => {
    TestBed.configureTestingModule({ teardown: { destroyAfterEach: false } });
    service = TestBed.inject(ScrollService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should be call scrollToAnchor with id', done => {
    document.getElementById = jest.fn(() => document.createElement('a'));
    window.getComputedStyle = jest.fn(() => ({ height: '42' } as any));
    window.scrollBy = jest.fn(() => null);

    const anchorId = 'anchorId';

    service.scrollToAnchor(anchorId).finally(() => {
      expect(document.getElementById).toBeCalledTimes(1);
      expect(document.getElementById).toBeCalledWith(anchorId);
      expect(window.scrollBy).toBeCalledTimes(2);
      done();
    });
  });

  it('should be call scrollToAnchor with xpath', done => {
    document.querySelector = jest.fn(() => document.createElement('a'));
    window.getComputedStyle = jest.fn(() => ({ height: '42' } as any));
    window.scrollBy = jest.fn(() => null);

    const anchorId = '#anchorId';

    service.scrollToAnchor(anchorId).finally(() => {
      expect(document.querySelector).toBeCalledWith(anchorId);
      expect(window.scrollBy).toBeCalledTimes(2);
      done();
    });
  });

  it('should be call scrollToAnchor with name attribute', done => {
    window.getComputedStyle = jest.fn(() => ({ height: '42' } as any));
    window.scrollBy = jest.fn(() => null);
    document.querySelector = jest
      .fn()
      .mockImplementationOnce(() => null)
      .mockImplementationOnce(() => document.createElement('a'));

    const anchorId = '#anchorId';

    service.scrollToAnchor(anchorId).finally(() => {
      expect(document.querySelector).toBeCalledWith(anchorId);
      expect(document.querySelector).toBeCalledWith(`a[name=${anchorId.substr(1)}]`);
      expect(window.scrollBy).toBeCalledTimes(2);
      done();
    });
  });
});
