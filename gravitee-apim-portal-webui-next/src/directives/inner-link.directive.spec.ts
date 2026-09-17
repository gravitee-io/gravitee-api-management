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
import { Component, input, signal, ViewEncapsulation } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

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
