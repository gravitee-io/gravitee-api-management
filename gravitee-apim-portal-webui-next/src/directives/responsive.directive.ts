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

import { Directive, Input, OnInit, OnDestroy, TemplateRef, ViewContainerRef } from '@angular/core';
import { Subscription } from 'rxjs';

import { ResponsiveService, ScreenSize } from '../services/responsive.service';

@Directive({
  selector: '[appResponsive]',
  standalone: true,
})
export class ResponsiveDirective implements OnInit, OnDestroy {
  @Input() appResponsive: ScreenSize | ScreenSize[] = ScreenSize.MD;
  @Input() appResponsiveMin?: ScreenSize;
  @Input() appResponsiveMax?: ScreenSize;
  @Input() appResponsiveHide?: boolean = false;

  private subscription?: Subscription;
  private hasView = false;

  constructor(
    private templateRef: TemplateRef<unknown>,
    private viewContainer: ViewContainerRef,
    private responsiveService: ResponsiveService,
  ) {}

  ngOnInit(): void {
    this.subscription = this.responsiveService.screenSize$.subscribe(screenInfo => {
      const shouldShow = this.shouldShowElement(screenInfo.size);

      if (shouldShow && !this.hasView) {
        this.viewContainer.createEmbeddedView(this.templateRef);
        this.hasView = true;
      } else if (!shouldShow && this.hasView) {
        this.viewContainer.clear();
        this.hasView = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  private shouldShowElement(currentSize: ScreenSize): boolean {
    // Handle min/max range
    if (this.appResponsiveMin || this.appResponsiveMax) {
      const sizeOrder = [ScreenSize.XS, ScreenSize.SM, ScreenSize.MD, ScreenSize.LG, ScreenSize.XL];
      const currentIndex = sizeOrder.indexOf(currentSize);

      if (this.appResponsiveMin) {
        const minIndex = sizeOrder.indexOf(this.appResponsiveMin);
        if (currentIndex < minIndex) return false;
      }

      if (this.appResponsiveMax) {
        const maxIndex = sizeOrder.indexOf(this.appResponsiveMax);
        if (currentIndex > maxIndex) return false;
      }

      return !this.appResponsiveHide;
    }

    // Handle specific sizes
    if (Array.isArray(this.appResponsive)) {
      const shouldShow = this.appResponsive.includes(currentSize);
      return this.appResponsiveHide ? !shouldShow : shouldShow;
    } else {
      const shouldShow = currentSize === this.appResponsive;
      return this.appResponsiveHide ? !shouldShow : shouldShow;
    }
  }
}
