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
import { Page } from 'projects/portal-webclient-sdk/src/lib';
import * as React from 'react';

import { GvPageSwaggerUIComponent } from './gv-page-swaggerui.component';

describe('GvPageSwaggerUIComponent', () => {
  const createComponent = createComponentFactory({
    component: GvPageSwaggerUIComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [HttpClientTestingModule],
  });

  let spectator: Spectator<GvPageSwaggerUIComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should return highlight plugin when try-it is enabled', () => {
    const page = {
      configuration: { try_it: true },
    } as Page;

    jest.spyOn(component as any, 'isTryItEnabled').mockReturnValue(true);
    const plugins = component.buildPlugins(page);
    expect(plugins.length).toBe(1);
    const plugin = plugins[0] as any;
    expect(plugin.components?.HighlightCode).toBeDefined();
    const HighlightCode = plugin.components.HighlightCode;
    const instance = new HighlightCode({ children: 'Sample' });
    const element = instance.render() as React.ReactElement;

    expect(element.type).toBe('pre');
    expect(element.props.style.backgroundColor).toBe('#2d2d2d');
    expect(element.props.children).toBe('Sample');
  });

  it('should return disabled plugins when try-it is disabled', () => {
    const page = {
      configuration: { try_it: false },
    } as Page;

    jest.spyOn(component as any, 'isTryItEnabled').mockReturnValue(false);
    const plugins = component.buildPlugins(page);

    expect(plugins.length).toBe(2);
    expect(typeof plugins[0]).toBe('function');
    expect(typeof plugins[1]).toBe('function');
  });
});
