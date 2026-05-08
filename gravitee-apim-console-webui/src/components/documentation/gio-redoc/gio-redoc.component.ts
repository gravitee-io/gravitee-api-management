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
import { Component, ElementRef, OnDestroy, effect, inject, input, viewChild } from '@angular/core';
import * as yaml from 'js-yaml';

import { GioRedocService, type RedocApi } from './gio-redoc.service';

const yamlSchema = yaml.DEFAULT_SCHEMA.extend([]);

const parseSpec = (spec: string): object | null => {
  try {
    const result: unknown = JSON.parse(spec);
    return result instanceof Object ? result : null;
  } catch {
    const result: unknown = yaml.load(spec, { schema: yamlSchema });
    return result instanceof Object ? result : null;
  }
};

@Component({
  selector: 'gio-redoc',
  templateUrl: './gio-redoc.component.html',
  styleUrls: ['./gio-redoc.component.scss'],
  standalone: true,
})
export class GioRedocComponent implements OnDestroy {
  private readonly gioRedocService = inject(GioRedocService);

  spec = input('');
  tryItURL = input('');

  private readonly redocNode = viewChild<ElementRef<HTMLElement>>('redoc');

  private pendingTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private initCallId = 0;

  private readonly initEffect = effect(() => {
    this.spec();
    this.tryItURL();
    const redocNode = this.redocNode();
    if (redocNode) {
      this.scheduleInit();
    }
  });

  ngOnDestroy(): void {
    if (this.pendingTimeoutId !== null) {
      clearTimeout(this.pendingTimeoutId);
      this.pendingTimeoutId = null;
    }
  }

  private scheduleInit(): void {
    if (this.pendingTimeoutId !== null) {
      clearTimeout(this.pendingTimeoutId);
    }
    this.pendingTimeoutId = setTimeout(() => {
      this.pendingTimeoutId = null;
      this.initRedoc();
    }, 300);
  }

  private async initRedoc(): Promise<void> {
    const callId = ++this.initCallId;
    const specValue = this.spec();
    const redocNode = this.redocNode();
    if (!specValue || !redocNode) return;

    let redoc: RedocApi;
    try {
      redoc = await this.gioRedocService.load();
    } catch (err) {
      // eslint-disable-next-line angular/log
      console.error('[gio-redoc] Failed to load Redoc script:', err);
      return;
    }

    if (callId !== this.initCallId) return;

    const tryItURL = this.tryItURL();
    const parsedSpec = parseSpec(specValue);
    if (parsedSpec === null) return;
    const spec = tryItURL ? { ...parsedSpec, servers: [{ url: tryItURL }] } : parsedSpec;
    redocNode.nativeElement.innerHTML = '';
    redoc.init(
      spec,
      {
        hideDownloadButton: true,
        hideLoading: true,
        disableSearch: true,
        onlyRequiredInSamples: true,
        nativeScrollbars: true,
        theme: {
          sidebar: { width: '150px' },
          breakpoints: { medium: '599rem', large: '600rem' },
        },
      },
      redocNode.nativeElement,
      err => {
        // eslint-disable-next-line angular/log
        if (err) console.error('[gio-redoc] Redoc.init failed:', err);
      },
    );
  }
}
