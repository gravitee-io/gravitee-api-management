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
import { APP_BASE_HREF, Location } from '@angular/common';
import { Component, input, signal, ViewEncapsulation } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { InnerLinkDirective } from './inner-link.directive';

@Component({
  selector: 'app-test-shadow-markdown',
  template: `
    <a href="#section-two"><strong data-testid="anchor-label">Go to section two</strong></a>
    <a href="/catalog" data-testid="internal-link">Catalog</a>
    <a href="https://gravitee.io" data-testid="external-link">Gravitee</a>
    @if (showTarget()) {
      <h2 id="section-two">Section Two</h2>
    }
  `,
  encapsulation: ViewEncapsulation.ShadowDom,
})
class TestShadowMarkdownComponent {
  showTarget = input(true);
}

@Component({
  template: `<app-test-shadow-markdown appInnerLink [showTarget]="showTarget()" />`,
  imports: [InnerLinkDirective, TestShadowMarkdownComponent],
})
class TestHostComponent {
  showTarget = signal(true);
}

const createRouterStub = (url: string) => ({
  url,
  config: [{ path: 'catalog' }],
  navigateByUrl: jest.fn().mockResolvedValue(true),
});

describe('InnerLinkDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let router: ReturnType<typeof createRouterStub>;
  let shadowRoot: ShadowRoot;
  let scrollIntoView: jest.Mock;
  const originalScrollIntoView = HTMLElement.prototype.scrollIntoView;

  const init = async ({ url = '/documentation?selectedId=page', showTarget = true } = {}) => {
    router = createRouterStub(url);
    scrollIntoView = jest.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', { configurable: true, value: scrollIntoView });

    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [{ provide: Router, useValue: router }],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.showTarget.set(showTarget);
    fixture.detectChanges();

    const shadowHost = fixture.nativeElement.querySelector('app-test-shadow-markdown') as HTMLElement;
    shadowRoot = shadowHost.shadowRoot!;
  };

  afterEach(() => {
    fixture.destroy();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', { configurable: true, value: originalScrollIntoView });
  });

  it('should preserve the current route and scroll to an anchor inside Shadow DOM', async () => {
    await init();

    const anchorLabel = shadowRoot.querySelector<HTMLElement>('[data-testid="anchor-label"]')!;
    const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true, composed: true });
    anchorLabel.dispatchEvent(clickEvent);
    await fixture.whenStable();

    expect(clickEvent.defaultPrevented).toBe(true);
    expect(router.navigateByUrl).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/documentation?selectedId=page#section-two');
    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'start' });
  });

  it('should scroll after asynchronously rendered content contains the URL fragment target', async () => {
    await init({ url: '/documentation?selectedId=page#section-two', showTarget: false });
    expect(scrollIntoView).not.toHaveBeenCalled();

    fixture.componentInstance.showTarget.set(true);
    fixture.detectChanges();
    await fixture.whenStable();
    await Promise.resolve();

    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'start' });
  });

  it('should scroll to the URL fragment target after a hash change', async () => {
    await init();
    expect(scrollIntoView).not.toHaveBeenCalled();

    router.url = '/documentation?selectedId=page#section-two';
    window.dispatchEvent(new Event('hashchange'));

    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'start' });
  });

  it('should keep navigating regular internal links through the router', async () => {
    await init();

    const internalLink = shadowRoot.querySelector<HTMLElement>('[data-testid="internal-link"]')!;
    const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true, composed: true });
    internalLink.dispatchEvent(clickEvent);

    expect(clickEvent.defaultPrevented).toBe(true);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/catalog');
    expect(scrollIntoView).not.toHaveBeenCalled();
  });

  it('should leave external links to the browser', async () => {
    await init();

    const externalLink = shadowRoot.querySelector<HTMLElement>('[data-testid="external-link"]')!;
    const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true, composed: true });
    externalLink.dispatchEvent(clickEvent);

    expect(clickEvent.defaultPrevented).toBe(false);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(scrollIntoView).not.toHaveBeenCalled();
  });
});

@Component({ template: 'page' })
class PageComponent {}

@Component({ template: 'not found' })
class NotFoundComponent {}

@Component({
  template: `<div appInnerLink>
    <a data-testid="link" [attr.href]="href" [attr.target]="target" [attr.download]="download">link</a>
  </div>`,
  imports: [InnerLinkDirective],
})
class LinkHostComponent {
  href = '';
  target: string | null = null;
  download: string | null = null;
}

describe('InnerLinkDirective with absolute paths', () => {
  const click = async ({
    baseHref,
    href,
    target = null,
    download = null,
    eventInit = {},
  }: {
    baseHref: string;
    href: string;
    target?: string | null;
    download?: string | null;
    eventInit?: MouseEventInit;
  }) => {
    await TestBed.configureTestingModule({
      imports: [LinkHostComponent],
      providers: [
        { provide: APP_BASE_HREF, useValue: baseHref },
        provideRouter([
          { path: '', component: PageComponent },
          { path: 'catalog', children: [{ path: '**', component: PageComponent }] },
          { path: 'documentation/:navId', component: PageComponent },
          { path: '', children: [{ path: 'guides/:id', component: PageComponent }] },
          { path: '**', component: NotFoundComponent },
        ]),
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    const location = TestBed.inject(Location);
    const fixture = TestBed.createComponent(LinkHostComponent);
    Object.assign(fixture.componentInstance, { href, target, download });
    fixture.detectChanges();
    const navigateByUrl = jest.spyOn(router, 'navigateByUrl');

    const event = new MouseEvent('click', { bubbles: true, cancelable: true, composed: true, button: 0, ...eventInit });
    fixture.nativeElement.querySelector('[data-testid="link"]').dispatchEvent(event);
    await fixture.whenStable();

    return { event, navigateByUrl, browserUrl: () => location.prepareExternalUrl(location.path()) };
  };

  it('should leave a same-host path outside the portal routes to the browser', async () => {
    const { event, navigateByUrl } = await click({ baseHref: '/', href: '/portal-extension/next/documentation/abc' });

    expect(event.defaultPrevented).toBe(false);
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('should not prepend the base href a second time to a link that already contains it', async () => {
    const { event, navigateByUrl, browserUrl } = await click({ baseHref: '/next/', href: '/next/documentation/abc' });

    expect(event.defaultPrevented).toBe(true);
    expect(navigateByUrl).toHaveBeenCalledWith('/documentation/abc');
    expect(browserUrl()).toBe('/next/documentation/abc');
  });

  it('should strip a multi-segment base href before routing', async () => {
    const { event, navigateByUrl } = await click({
      baseHref: '/portal-extension/next/',
      href: '/portal-extension/next/documentation/abc',
    });

    expect(event.defaultPrevented).toBe(true);
    expect(navigateByUrl).toHaveBeenCalledWith('/documentation/abc');
  });

  it('should only strip the base href on a segment boundary', async () => {
    const { event, navigateByUrl } = await click({ baseHref: '/next/', href: '/nextgen/documentation/abc' });

    expect(event.defaultPrevented).toBe(false);
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('should strip a base href whose first segment is also a route name', async () => {
    const { navigateByUrl } = await click({ baseHref: '/documentation/', href: '/documentation/documentation/abc' });

    expect(navigateByUrl).toHaveBeenCalledWith('/documentation/abc');
  });

  it.each(['/', '/next/'])('should route generated internal links through the router at base %s', async baseHref => {
    for (const href of ['/catalog/api/a/documentation/p', '/catalog']) {
      TestBed.resetTestingModule();
      const { event, navigateByUrl } = await click({ baseHref, href });

      expect(event.defaultPrevented).toBe(true);
      expect(navigateByUrl).toHaveBeenCalledWith(href);
    }
  });

  it('should route links to children of a top-level empty-path route', async () => {
    const { event, navigateByUrl } = await click({ baseHref: '/', href: '/guides/abc' });

    expect(event.defaultPrevented).toBe(true);
    expect(navigateByUrl).toHaveBeenCalledWith('/guides/abc');
  });

  it('should route a link to the portal root with target _self', async () => {
    const { event, navigateByUrl } = await click({ baseHref: '/', href: '/', target: '_self' });

    expect(event.defaultPrevented).toBe(true);
    expect(navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('should route a full link to the portal itself without reloading the page', async () => {
    const { event, navigateByUrl } = await click({ baseHref: '/next/', href: `${window.location.origin}/next/catalog` });

    expect(event.defaultPrevented).toBe(true);
    expect(navigateByUrl).toHaveBeenCalledWith('/catalog');
  });

  it('should route a dot-relative link', async () => {
    window.history.replaceState({}, '', '/');
    const { event, navigateByUrl } = await click({ baseHref: '/', href: './catalog' });

    expect(event.defaultPrevented).toBe(true);
    expect(navigateByUrl).toHaveBeenCalledWith('/catalog');
  });

  it('should keep the current page for a query-only link', async () => {
    window.history.replaceState({}, '', '/documentation/abc');
    const { event, navigateByUrl } = await click({ baseHref: '/', href: '?tab=2' });

    expect(event.defaultPrevented).toBe(true);
    expect(navigateByUrl).toHaveBeenCalledWith('/documentation/abc?tab=2');
  });

  it.each([
    ['a mailto link', { href: 'mailto:x@y' }],
    ['a tel link', { href: 'tel:+123' }],
    ['a protocol-relative link', { href: '//other.host/catalog' }],
    ['a javascript link', { href: 'javascript:void(0)' }],
    ['a ctrl-click', { href: '/catalog', eventInit: { ctrlKey: true } }],
    ['a meta-click', { href: '/catalog', eventInit: { metaKey: true } }],
    ['a middle-click', { href: '/catalog', eventInit: { button: 1 } }],
    ['a link opening in a new tab', { href: '/catalog', target: '_blank' }],
    ['a download link', { href: '/catalog', download: '' }],
  ])('should leave %s to the browser', async (_, options) => {
    const { event, navigateByUrl } = await click({ baseHref: '/', ...options });

    expect(event.defaultPrevented).toBe(false);
    expect(navigateByUrl).not.toHaveBeenCalled();
  });
});
