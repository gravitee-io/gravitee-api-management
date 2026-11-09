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
import { GraviteeMarkdownViewerModule, resolveElExpressions } from '@gravitee/gravitee-markdown';
import { toSvg } from 'jdenticon';

import { Component, computed, effect, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';

import { DEFAULT_CATALOG_CARD_TEMPLATE } from './default-catalog-card-template';

function generatedApiPicture(hashValue: string, size = 48): string {
  const asSvg = toSvg(hashValue, size, { backColor: '#FFF', padding: 0 });
  return `data:image/svg+xml;base64,${window.btoa(asSvg)}`;
}

@Component({
  selector: 'app-catalog-api-card',
  imports: [GraviteeMarkdownViewerModule, MatButtonModule, MatTooltip],
  templateUrl: './catalog-api-card.component.html',
  styleUrl: './catalog-api-card.component.scss',
})
export class CatalogApiCardComponent {
  readonly apiId = input.required<string>();
  readonly title = input.required<string>();
  readonly version = input('');
  readonly description = input('');
  readonly picture = input<string | undefined>();
  readonly typeLabel = input<string | undefined>();
  readonly status = input<string | undefined>();
  readonly ownerName = input<string | undefined>();
  readonly isEnabledMcpServer = input(false);
  readonly labels = input<string[]>([]);
  readonly metadata = input<Record<string, string | string[] | undefined>>({});
  readonly cardTemplate = input<string | undefined>();
  readonly showNotificationBell = input(false);
  readonly notificationsSubscribed = input(false);
  readonly notificationBusy = input(false);

  readonly cardSelect = output<string>();
  readonly notificationToggle = output<string>();

  /** Picture URL usable in GMD (`{#api.picture}`), with jdenticon fallback when missing/broken. */
  readonly resolvedPicture = signal('');

  constructor() {
    effect(onCleanup => {
      const url = this.picture();
      const fallback = generatedApiPicture(`${this.title()} ${this.version()}`);
      if (!url) {
        this.resolvedPicture.set(fallback);
        return;
      }

      this.resolvedPicture.set(url);
      const img = new Image();
      let cancelled = false;
      img.onerror = () => {
        if (!cancelled) {
          this.resolvedPicture.set(fallback);
        }
      };
      img.src = url;
      onCleanup(() => {
        cancelled = true;
      });
    });
  }

  readonly resolvedContent = computed(() =>
    resolveElExpressions(this.cardTemplate()?.trim() || DEFAULT_CATALOG_CARD_TEMPLATE, {
      api: {
        name: this.title(),
        version: this.version(),
        description: this.description(),
        picture: this.resolvedPicture(),
        type: this.typeLabel(),
        status: this.status(),
        owner: this.ownerName(),
        mcp: this.isEnabledMcpServer(),
        labels: this.labels(),
        metadata: this.metadata(),
      },
    }),
  );

  notificationBellAriaLabel(): string {
    return this.notificationsSubscribed()
      ? $localize`:@@apiCardNotifyUnsubscribeAria:Unsubscribe from notifications for ${this.title()}:apiName:`
      : $localize`:@@apiCardNotifySubscribeAria:Subscribe to notifications for ${this.title()}:apiName:`;
  }

  notificationBellTooltip(): string {
    return this.notificationsSubscribed()
      ? $localize`:@@apiCardNotifyUnsubscribeTooltip:Notifications on — click to turn off`
      : $localize`:@@apiCardNotifySubscribeTooltip:Get notified about this API`;
  }

  onCardActivate(): void {
    this.cardSelect.emit(this.apiId());
  }

  onNotifyClick(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.notificationBusy()) {
      return;
    }
    this.notificationToggle.emit(this.apiId());
  }

  onNotifyKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      event.stopPropagation();
      if (!this.notificationBusy()) {
        this.notificationToggle.emit(this.apiId());
      }
    }
  }
}
