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
import { Directive, ElementRef, HostListener, inject } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';

/**
 * Enables {@link MatTooltip} only when the host text overflows horizontally (ellipsis),
 * measured lazily on pointer or keyboard focus. No layout observers — prefer for long lists.
 *
 * @usage
 * <span [matTooltip]="label" appMatTooltipOnEllipsis>{{ label }}</span>
 */
@Directive({
  selector: '[matTooltip][appMatTooltipOnEllipsis]',
  standalone: true,
})
export class MatTooltipOnEllipsisDirective {
  private readonly matTooltip = inject(MatTooltip);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private originalDisabledState = false;

  @HostListener('mouseenter')
  @HostListener('focusin')
  checkOverflow(): void {
    this.originalDisabledState = this.matTooltip.disabled;
    if (this.originalDisabledState) {
      return;
    }
    const element = this.elementRef.nativeElement;
    const isTextTruncated = element.scrollWidth > element.clientWidth;
    this.matTooltip.disabled = !isTextTruncated;
  }

  @HostListener('mouseleave')
  @HostListener('focusout')
  resetDisabledState(): void {
    this.matTooltip.disabled = this.originalDisabledState;
  }
}
