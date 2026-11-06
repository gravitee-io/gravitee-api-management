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
import { Location } from '@angular/common';
import { AfterViewInit, Directive, ElementRef, HostListener, inject, OnDestroy } from '@angular/core';
import { Router, Routes } from '@angular/router';

@Directive({
  selector: '[appInnerLink]',
  standalone: true,
})
export class InnerLinkDirective implements AfterViewInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly hostElement = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  private pendingFragment: string | null = null;
  private mutationObserver?: MutationObserver;

  @HostListener('window:hashchange')
  public onHashChange(): void {
    this.syncFragmentFromUrl();
  }

  @HostListener('click', ['$event'])
  public onClick(event: MouseEvent): void {
    const anchor = this.findAnchor(event);
    const href = anchor?.getAttribute('href');

    if (!anchor || !href || this.isLeftToBrowser(event, anchor)) {
      return;
    }

    if (href.startsWith('#')) {
      event.preventDefault();

      const fragment = this.decodeFragment(href.substring(1));
      const currentUrl = this.router.url.split('#')[0];
      this.pendingFragment = fragment || null;

      this.observePendingFragment();
      void this.router.navigateByUrl(`${currentUrl}${href}`);
      this.scrollToPendingFragment();
      return;
    }

    // Normalizing the browser-resolved path strips the base href, so the router does not prepend it a second time.
    const appPath = this.location.normalize(anchor.pathname) || '/';
    if (this.matchesTopLevelRoute(appPath, this.router.config)) {
      event.preventDefault();
      void this.router.navigateByUrl(`${appPath}${anchor.search}${anchor.hash}`);
    }
  }

  public ngAfterViewInit(): void {
    this.syncFragmentFromUrl();
  }

  public ngOnDestroy(): void {
    this.mutationObserver?.disconnect();
  }

  private isLeftToBrowser(event: MouseEvent, anchor: HTMLAnchorElement): boolean {
    const target = anchor.getAttribute('target');
    return (
      event.button !== 0 ||
      event.ctrlKey ||
      event.metaKey ||
      event.shiftKey ||
      event.altKey ||
      anchor.hasAttribute('download') ||
      (!!target && target !== '_self') ||
      anchor.origin !== this.hostElement.ownerDocument.defaultView?.location.origin
    );
  }

  /** Paths outside the router config (another app on the same host) must reach the server, not the portal's 404 route. */
  private matchesTopLevelRoute(path: string, routes: Routes): boolean {
    const firstSegment = path.split(/[/;]/)[1];
    return routes.some(route => {
      if (route.path === '**') {
        return false;
      }
      if (!route.path) {
        if (firstSegment === '') {
          return true;
        }

        return !!route.children && this.matchesTopLevelRoute(path, route.children);
      }
      const routeSegment = route.path.split('/')[0];
      return routeSegment.startsWith(':') || routeSegment === firstSegment;
    });
  }

  private syncFragmentFromUrl(): void {
    this.pendingFragment = this.getCurrentFragment();
    this.observePendingFragment();
    this.scrollToPendingFragment();
  }

  private findAnchor(event: MouseEvent): HTMLAnchorElement | undefined {
    return event.composedPath().find((target): target is HTMLAnchorElement => target instanceof HTMLAnchorElement);
  }

  private getCurrentFragment(): string | null {
    const url = this.router.url;
    const fragment = url.includes('#')
      ? url.substring(url.indexOf('#') + 1)
      : this.hostElement.ownerDocument.defaultView?.location.hash.substring(1);

    if (!fragment) {
      return null;
    }

    return this.decodeFragment(fragment);
  }

  private decodeFragment(fragment: string): string {
    try {
      return decodeURIComponent(fragment);
    } catch {
      return fragment;
    }
  }

  private getContentRoot(): HTMLElement | ShadowRoot {
    return this.hostElement.shadowRoot ?? this.hostElement;
  }

  private observePendingFragment(): void {
    if (!this.pendingFragment || this.mutationObserver) {
      return;
    }

    const contentRoot = this.getContentRoot();
    const MutationObserverClass = contentRoot.ownerDocument.defaultView?.MutationObserver;

    if (MutationObserverClass) {
      this.mutationObserver = new MutationObserverClass(() => this.scrollToPendingFragment());
      this.mutationObserver.observe(contentRoot, { childList: true, subtree: true });
    }
  }

  private scrollToPendingFragment(): void {
    if (!this.pendingFragment) {
      return;
    }

    const target = Array.from(this.getContentRoot().querySelectorAll<HTMLElement>('[id]')).find(
      element => element.id === this.pendingFragment,
    );

    if (target) {
      target.scrollIntoView({ block: 'start' });
      this.pendingFragment = null;
      this.mutationObserver?.disconnect();
      this.mutationObserver = undefined;
    }
  }
}
