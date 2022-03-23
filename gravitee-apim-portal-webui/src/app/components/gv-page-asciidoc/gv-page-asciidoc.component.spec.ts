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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { SafePipe } from '../../pipes/safe.pipe';

import { GvPageAsciiDocComponent } from './gv-page-asciidoc.component';

describe('GvPageAsciiDocComponent', () => {
  const createComponent = createComponentFactory({
    component: GvPageAsciiDocComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    declarations: [SafePipe],
    imports: [HttpClientTestingModule],
  });

  let spectator: Spectator<GvPageAsciiDocComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
    component.pageContent = null;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
