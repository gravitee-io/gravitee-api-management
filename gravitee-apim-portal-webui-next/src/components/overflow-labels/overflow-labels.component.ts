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

import { isPlatformBrowser } from '@angular/common';
import {
  afterNextRender,
  afterRenderEffect,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  PLATFORM_ID,
  signal,
  viewChild,
} from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';

import { BadgeComponent } from '../badge/badge.component';

export function computeVisibleCount(badgeWidths: number[], containerWidth: number, counterWidth: number, gap: number): number {
  if (badgeWidths.length === 0 || containerWidth <= 0) {
    return 0;
  }

  let totalAll = 0;
  for (let i = 0; i < badgeWidths.length; i++) {
    totalAll += badgeWidths[i] + (i > 0 ? gap : 0);
  }
  if (totalAll <= containerWidth) {
    return badgeWidths.length;
  }

  let used = 0;
  let count = 0;
  for (const w of badgeWidths) {
    const next = used + (count > 0 ? gap : 0) + w;
    const counterSpace = gap + counterWidth;
    if (next + counterSpace > containerWidth) {
      break;
    }
    used = next;
    count++;
  }
  return count;
}

@Component({
  selector: 'app-overflow-labels',
  standalone: true,
  imports: [BadgeComponent, MatTooltip],
  templateUrl: './overflow-labels.component.html',
  styleUrl: './overflow-labels.component.scss',
})
export class OverflowLabelsComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  readonly labels = input.required<string[]>();
  readonly maxBadgeWidth = input(150);

  private readonly containerElement = viewChild.required<ElementRef<HTMLElement>>('container');
  private readonly measureElement = viewChild.required<ElementRef<HTMLElement>>('measure');
  private readonly containerWidth = signal(0);
  private readonly badgeWidths = signal<number[]>([], {
    equal: (a, b) => a.length === b.length && a.every((value, index) => value === b[index]),
  });
  private readonly counterWidth = signal(0);

  private readonly visibleCount = computed(() => computeVisibleCount(this.badgeWidths(), this.containerWidth(), this.counterWidth(), 4));
  protected readonly visibleLabels = computed(() => this.labels().slice(0, this.visibleCount()));
  protected readonly hiddenLabels = computed(() => this.labels().slice(this.visibleCount()));
  protected readonly hiddenLabelsTooltip = computed(() => this.hiddenLabels().join(', '));

  constructor() {
    if (!this.isBrowser) return;

    afterNextRender(() => {
      const element = this.containerElement().nativeElement;
      this.containerWidth.set(element.getBoundingClientRect().width);
      this.measureBadges();

      const resizeObserver = new ResizeObserver(entries => {
        this.containerWidth.set(entries[0]?.contentRect.width ?? 0);
        this.measureBadges();
      });
      resizeObserver.observe(element);
      this.destroyRef.onDestroy(() => resizeObserver.disconnect());
    });

    afterRenderEffect(() => {
      this.labels();
      this.measureBadges();
    });
  }

  private measureBadges(): void {
    const measureContainer = this.measureElement().nativeElement;
    const badgeElements = measureContainer.querySelectorAll<HTMLElement>('[data-role="measure-badge"]');
    this.badgeWidths.set(Array.from(badgeElements).map(element => element.getBoundingClientRect().width));
    const counterElement = measureContainer.querySelector<HTMLElement>('[data-role="measure-counter"]');
    if (counterElement) {
      this.counterWidth.set(counterElement.getBoundingClientRect().width);
    }
  }
}
