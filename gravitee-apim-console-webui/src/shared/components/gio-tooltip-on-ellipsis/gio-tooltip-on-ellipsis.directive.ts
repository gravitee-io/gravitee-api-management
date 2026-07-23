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
import { Directive, ElementRef, HostListener, Inject, NgZone, Optional, ViewContainerRef } from '@angular/core';
import { MAT_TOOLTIP_DEFAULT_OPTIONS, MAT_TOOLTIP_SCROLL_STRATEGY, MatTooltip, MatTooltipDefaultOptions } from '@angular/material/tooltip';
import { Overlay, ScrollDispatcher } from '@angular/cdk/overlay';
import { Platform } from '@angular/cdk/platform';
import { AriaDescriber, FocusMonitor } from '@angular/cdk/a11y';
import { Directionality } from '@angular/cdk/bidi';
import { DOCUMENT } from '@angular/common';

/**
 * Enables and displays a tooltip with the element's full text when that text is truncated with an ellipsis.
 */
@Directive({
  selector: '[gioTooltipOnEllipsis]',
  standalone: false,
})
export class GioTooltipOnEllipsisDirective extends MatTooltip {
  private nativeElement: HTMLElement;

  @HostListener('mouseenter')
  onMouseEnter(): void {
    this.syncFromElement();
    if (!this.disabled && this.message) {
      this.show();
    }
  }

  private syncFromElement(): void {
    const text = this.nativeElement?.textContent?.trim() ?? '';
    const isEllipsis = !!text && this.hasVisibleOverflow(this.nativeElement);

    this.message = isEllipsis ? text : '';
    this.disabled = !isEllipsis;
  }

  private hasVisibleOverflow(element: HTMLElement): boolean {
    const elementsToCheck = [
      element,
      element.closest<HTMLElement>('.mat-mdc-select-value-text'),
      element.closest<HTMLElement>('.mat-mdc-select-trigger'),
    ].filter((el, index, list): el is HTMLElement => !!el && list.indexOf(el) === index);

    for (const el of elementsToCheck) {
      if (el.offsetWidth < el.scrollWidth) {
        return true;
      }
    }

    const primaryText =
      element.querySelector<HTMLElement>('.mdc-list-item__primary-text') ?? element.closest<HTMLElement>('.mdc-list-item__primary-text');

    if (primaryText && primaryText !== element) {
      return primaryText.offsetWidth < primaryText.scrollWidth;
    }

    return false;
  }

  constructor(
    _overlay: Overlay,
    _elementRef: ElementRef<HTMLElement>,
    _scrollDispatcher: ScrollDispatcher,
    _viewContainerRef: ViewContainerRef,
    _ngZone: NgZone,
    _platform: Platform,
    _ariaDescriber: AriaDescriber,
    _focusMonitor: FocusMonitor,
    @Inject(MAT_TOOLTIP_SCROLL_STRATEGY) scrollStrategy: any,
    _dir: Directionality,
    @Optional() @Inject(MAT_TOOLTIP_DEFAULT_OPTIONS) _defaultOptions: MatTooltipDefaultOptions,
    @Inject(DOCUMENT) _document: any,
  ) {
    super(
      _overlay,
      _elementRef,
      _scrollDispatcher,
      _viewContainerRef,
      _ngZone,
      _platform,
      _ariaDescriber,
      _focusMonitor,
      scrollStrategy,
      _dir,
      _defaultOptions,
      _document,
    );
    this.nativeElement = _elementRef.nativeElement;
  }
}
