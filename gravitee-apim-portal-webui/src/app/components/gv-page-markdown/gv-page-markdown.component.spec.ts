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
import { RouterTestingModule } from '@angular/router/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { mockProvider } from '@ngneat/spectator/jest';

import { GvMarkdownTocComponent } from '../gv-markdown-toc/gv-markdown-toc.component';
import { SafePipe } from '../../pipes/safe.pipe';
import { ConfigurationService } from '../../services/configuration.service';

import { GvPageMarkdownComponent } from './gv-page-markdown.component';

const BASE_URL = 'my-base-url';

describe('GvPageMarkdownComponent', () => {
  const createComponent = createComponentFactory({
    component: GvPageMarkdownComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    declarations: [SafePipe, GvMarkdownTocComponent],
    imports: [RouterTestingModule],
    providers: [
      mockProvider(ConfigurationService, {
        get: () => BASE_URL,
      }),
    ],
  });

  let spectator: Spectator<GvPageMarkdownComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
    component.withToc = null;
    component.pageContent = null;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should use correct portal media url', () => {
    const renderer = component.renderer.image(
      'https://host:port/contextpath/management/organizations/DEFAULT/environments/DEFAULT/portal/media/123456789',
      'title',
      'text',
    );

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual(`<img alt="text" title="title" src="${BASE_URL}/media/123456789" />`);
  });

  it('should use correct api media url', () => {
    const renderer = component.renderer.image(
      'https://host:port/contextpath/management/organizations/DEFAULT/environments/DEFAULT/apis/1A2Z3E4R5T6Y/media/123456789',
      'title',
      'text',
    );

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual(`<img alt="text" title="title" src="${BASE_URL}/apis/1A2Z3E4R5T6Y/media/123456789" />`);
  });

  it('should use gv-button[data-page-id] for render an portal page link', () => {
    const renderer = component.renderer.link('/#!/settings/pages/123456789', 'title', 'text');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<gv-button link data-page-id="123456789">text</gv-button>');
  });

  it('should use gv-button[data-page-id] for render an api page link', () => {
    const renderer = component.renderer.link('/#!/apis/1A2Z3E4R5T6Y/documentation/123456789', 'title', 'text');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<gv-button link data-page-id="123456789">text</gv-button>');
  });

  it('should use gv-button[data-page-id] for render an portal page link', () => {
    const renderer = component.renderer.link('/#!/settings/pages/123456789', 'title', 'text');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<gv-button link data-page-id="123456789">text</gv-button>');
  });

  it('should use gv-button[data-page-id] for render an api page link', () => {
    const renderer = component.renderer.link('/#!/apis/1A2Z3E4R5T6Y/documentation/123456789', 'title', 'text');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<gv-button link data-page-id="123456789">text</gv-button>');
  });

  it('should use gv-button[href] for render an anchor', () => {
    const renderer = component.renderer.link('#anchor', 'Anchor', '');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<gv-button link href="#anchor"></gv-button>');
  });

  it('should call onButtonClick when click to gv-button[href]', () => {
    const anchor = '#anchor';
    component.onButtonClick = jest.fn();

    const button = document.createElement('gv-button');
    button.setAttribute('href', anchor);
    spectator.element.appendChild(button);
    button.click();

    expect(component.onButtonClick).toBeCalledTimes(1);
    expect(component.onButtonClick).toBeCalledWith(button);
  });
});
