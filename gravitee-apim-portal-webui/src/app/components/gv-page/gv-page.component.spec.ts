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
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { SafePipe } from '../../pipes/safe.pipe';

import { GvPageComponent } from './gv-page.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { GvPageRedocComponent } from '../gv-page-redoc/gv-page-redoc.component';
import { GvPageSwaggerUIComponent } from '../gv-page-swaggerui/gv-page-swaggerui.component';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';
import { GvPageContentSlotDirective } from 'src/app/directives/gv-page-content-slot.directive';
import { GvPageMarkdownComponent } from '../gv-page-markdown/gv-page-markdown.component';

describe('GvPageComponent', () => {
  const createComponent = createComponentFactory({
    component: GvPageComponent,
    declarations: [GvPageMarkdownComponent, GvPageRedocComponent, GvPageSwaggerUIComponent, GvPageContentSlotDirective, SafePipe],
    imports: [HttpClientTestingModule, RouterTestingModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    overrideModules: [
      [
        BrowserDynamicTestingModule,
        {
          set: {
            entryComponents: [GvPageMarkdownComponent, GvPageRedocComponent, GvPageSwaggerUIComponent],
          },
        },
      ],
    ],
  });

  let spectator: Spectator<GvPageComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
    component.page = null;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
