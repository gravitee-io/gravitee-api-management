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
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { GvMarkdownTocComponent } from '../gv-markdown-toc/gv-markdown-toc.component';
import { SafePipe } from '../../pipes/safe.pipe';
import { ConfigurationService } from '../../services/configuration.service';
import { PageService } from '../../services/page.service';
import { Page } from '../../../../projects/portal-webclient-sdk/src/lib';
import { ScrollService } from '../../services/scroll.service';

import { GvPageMarkdownComponent } from './gv-page-markdown.component';

const BASE_URL = 'my-base-url';
const PAGE_BASE_URL = '/catalog/api/1234/doc';

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
      mockProvider(PageService, {
        getCurrentPage: () => docPage,
      }),
    ],
  });

  let spectator: Spectator<GvPageMarkdownComponent>;
  let component: GvPageMarkdownComponent;
  const docPage: Page = { name: 'A Page', id: '86de4f08-aa02-40f0-aa73-4b3e0e97fef4', content: '', type: 'MARKDOWN', order: 1 };

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
    component.withToc = null;
    component.pageContent = null;
    component.pageBaseUrl = PAGE_BASE_URL;
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

  it('should use a.internal-link for render an portal page link', () => {
    const renderer = component.renderer.link('/#!/settings/pages/123456789', 'title', 'text');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=123456789">text</a>');
  });

  it('should use a.internal-link for render an api page link', () => {
    const renderer = component.renderer.link('/#!/apis/1A2Z3E4R5T6Y/documentation/123456789', 'title', 'text');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=123456789">text</a>');
  });

  it('should use a.internal-link for render an portal page link', () => {
    const renderer = component.renderer.link('/#!/settings/pages/123456789', 'title', 'text');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=123456789">text</a>');
  });

  it('should use a.internal-link for render an api page link', () => {
    const renderer = component.renderer.link('/#!/apis/1A2Z3E4R5T6Y/documentation/123456789', 'title', 'text');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=123456789">text</a>');
  });

  it('should use a.anchor for render an anchor', () => {
    const renderer = component.renderer.link('#anchor', 'Anchor', '');

    expect(renderer).not.toBeNull();
    expect(renderer).toEqual('<a class="anchor" href="#anchor"></a>');
  });

  it('should call scroll to anchor when click to a.anchor', () => {
    const anchor = '#anchor';
    const scrollToAnchorSpy = jest.spyOn(TestBed.inject(ScrollService), 'scrollToAnchor').mockReturnValue(new Promise(() => true));

    const linkElement = document.createElement('a');
    linkElement.className = 'anchor';
    linkElement.setAttribute('href', anchor);
    spectator.element.appendChild(linkElement);
    linkElement.click();

    expect(scrollToAnchorSpy).toBeCalledTimes(1);
    expect(scrollToAnchorSpy).toBeCalledWith(anchor);
  });

  it('should call navigate to page when click to a.internal-link', () => {
    const pageLink = '/api/1234/doc?page=35365';
    const navigateByUrlSpy = jest.spyOn(TestBed.inject(Router), 'navigateByUrl').mockReturnValue(new Promise(() => true));

    const internalLinkElement = document.createElement('a');
    internalLinkElement.className = 'internal-link';
    internalLinkElement.setAttribute('href', pageLink);
    spectator.element.appendChild(internalLinkElement);
    internalLinkElement.click();

    expect(navigateByUrlSpy).toBeCalledTimes(1);
    expect(navigateByUrlSpy).toBeCalledWith(pageLink);
  });

  it('should open external link', () => {
    const externalLink = 'https://www.gravitee.io/';
    const navigateByUrlSpy = jest.spyOn(TestBed.inject(Router), 'navigateByUrl');
    const scrollToAnchorSpy = jest.spyOn(TestBed.inject(ScrollService), 'scrollToAnchor');

    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', externalLink);
    spectator.element.appendChild(linkElement);
    linkElement.click();

    expect(navigateByUrlSpy).not.toBeCalled();
    expect(scrollToAnchorSpy).not.toBeCalled();
  });
});
