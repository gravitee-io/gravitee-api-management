/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { BreadcrumbService } from './breadcrumb.service';
import { Breadcrumb } from '../components/breadcrumbs/breadcrumbs.component';

describe('BreadcrumbService', () => {
  let service: BreadcrumbService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BreadcrumbService);
  });

  it('should start with empty breadcrumbs', () => {
    expect(service.breadcrumbs()).toEqual([]);
  });

  it('should set breadcrumbs', () => {
    const crumbs: Breadcrumb[] = [
      { id: 'home', label: 'Home', url: '/' },
      { id: 'apps', label: 'Applications' },
    ];

    service.set(crumbs);

    expect(service.breadcrumbs()).toEqual(crumbs);
  });

  it('should replace breadcrumbs when set again', () => {
    service.set([{ id: 'a', label: 'A' }]);
    const next: Breadcrumb[] = [{ id: 'b', label: 'B', url: '/b' }];

    service.set(next);

    expect(service.breadcrumbs()).toEqual(next);
  });

  it('should clear breadcrumbs', () => {
    service.set([{ id: 'x', label: 'X' }]);

    service.clear();

    expect(service.breadcrumbs()).toEqual([]);
  });
});
