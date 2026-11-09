/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { NgClass } from '@angular/common';
import {
  afterNextRender,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnDestroy,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Page } from '../../../entities/page/page';
import { ConfigService } from '../../../services/config.service';
import { MarkdownService } from '../../../services/markdown.service';
import {
  GraviteeOperation,
  GraviteeSpecView,
  buildRequestUrl,
  downloadSpecContent,
  findOperationSectionElement,
  getDocsScrollContainer,
  getDocsStickyHeaderOffset,
  getOperationSectionId,
  parseGraviteeSpec,
  scrollOperationSectionIntoView,
} from './page-gravitee.models';

interface TryItResponse {
  status: number;
  statusText: string;
  durationMs: number;
  body: string;
  ok: boolean;
}

const CLICK_SCROLL_LOCK_MS = 800;

@Component({
  selector: 'app-page-gravitee',
  standalone: true,
  imports: [FormsModule, NgClass],
  templateUrl: './page-gravitee.component.html',
  styleUrl: './page-gravitee.component.scss',
})
export class PageGraviteeComponent implements OnDestroy {
  private readonly pageSignal = signal<Page | undefined>(undefined);
  private readonly markdownService = inject(MarkdownService);
  private readonly configService = inject(ConfigService);
  private readonly domSanitizer = inject(DomSanitizer);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly ngZone = inject(NgZone);

  private readonly docsRoot = viewChild<ElementRef<HTMLElement>>('docsRoot');
  private readonly docsHeader = viewChild<ElementRef<HTMLElement>>('docsHeader');

  private readonly visibleSections = new Map<string, IntersectionObserverEntry>();
  private intersectionObserver?: IntersectionObserver;
  private headerResizeObserver?: ResizeObserver;
  private isClickScrolling = false;
  private clickScrollTimeoutId: number | null = null;
  private observedOperationIdsKey = '';

  @Input({ required: true })
  set page(value: Page) {
    this.pageSignal.set(value);
  }

  readonly schemasExpanded = signal(true);
  readonly endpointSearch = signal('');
  readonly activeOperationId = signal<string | null>(null);
  readonly pathValues = signal<Record<string, string>>({});
  readonly queryValues = signal<Record<string, string>>({});
  readonly headerValues = signal<Record<string, string>>({});
  readonly body = signal('{\n  \n}');
  readonly isSending = signal(false);
  readonly tryItError = signal<string | null>(null);
  readonly tryItResponse = signal<TryItResponse | null>(null);
  readonly copied = signal(false);
  readonly selectedServerUrl = signal('');

  readonly view = computed<GraviteeSpecView | null>(() => {
    const page = this.pageSignal();
    if (!page?.content) {
      return null;
    }
    return parseGraviteeSpec(page.content);
  });

  readonly descriptionHtml = computed<SafeHtml | null>(() => {
    const description = this.view()?.description?.trim();
    if (!description) {
      return null;
    }
    const html = this.markdownService.render(description, this.configService.baseURL, '', []);
    return this.domSanitizer.bypassSecurityTrustHtml(html);
  });

  readonly activeOperation = computed<GraviteeOperation | null>(() => {
    const spec = this.view();
    if (!spec?.operations.length) {
      return null;
    }
    return spec.operations.find(operation => operation.id === this.activeOperationId()) ?? spec.operations[0];
  });

  readonly filteredOperationsByTag = computed(() => {
    const spec = this.view();
    if (!spec) {
      return [];
    }

    const query = this.endpointSearch().trim().toLowerCase();
    if (!query) {
      return spec.operationsByTag;
    }

    return spec.operationsByTag
      .map(group => ({
        tag: group.tag,
        operations: group.operations.filter(operation => {
          const haystack = [operation.summary, operation.path, operation.method, operation.tag, operation.id]
            .join(' ')
            .toLowerCase();
          return haystack.includes(query);
        }),
      }))
      .filter(group => group.operations.length > 0);
  });

  readonly configuredTryItUrl = computed(() => this.pageSignal()?.configuration?.try_it_url?.trim() ?? '');

  readonly serverUrl = computed(() => {
    const override = this.configuredTryItUrl();
    if (override) {
      return override;
    }
    const selected = this.selectedServerUrl().trim();
    if (selected) {
      return selected;
    }
    return this.view()?.servers[0] ?? '';
  });

  readonly requestUrl = computed(() => {
    const operation = this.activeOperation();
    if (!operation) {
      return '';
    }
    return buildRequestUrl(operation, this.serverUrl(), this.pathValues(), this.queryValues());
  });

  readonly codeSample = computed(() => {
    const operation = this.activeOperation();
    if (!operation) {
      return '';
    }
    const url = this.requestUrl() || `${this.serverUrl()}${operation.path}`;
    const method = operation.method;
    const headers = Object.entries(this.headerValues())
      .filter(([, value]) => value)
      .map(([name, value]) => `  -H '${name}: ${value}'`)
      .join(' \\\n');
    const body = operation.hasRequestBody && this.body().trim() ? `  -d '${this.body().replace(/'/g, `'\\''`)}'` : '';
    return [`curl -X ${method} '${url}'`, headers, body].filter(Boolean).join(' \\\n');
  });

  constructor() {
    effect(
      () => {
        const spec = this.view();
        if (!spec?.operations.length) {
          this.activeOperationId.set(null);
          return;
        }
        const current = this.activeOperationId();
        if (!current || !spec.operations.some(operation => operation.id === current)) {
          this.applyOperationState(spec.operations[0], { resetTryIt: true });
        }
        if (!this.selectedServerUrl() && spec.servers[0]) {
          this.selectedServerUrl.set(spec.servers[0]);
        }
      },
      { allowSignalWrites: true },
    );

    afterNextRender(() => {
      this.setupHeaderOffsetObserver();
      this.setupScrollSpy();
    });

    effect(() => {
      const operationIds = this.view()?.operations.map(operation => operation.id).join('|') ?? '';
      if (operationIds !== this.observedOperationIdsKey) {
        this.observedOperationIdsKey = operationIds;
        queueMicrotask(() => {
          this.setupHeaderOffsetObserver();
          this.setupScrollSpy();
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.teardownScrollSpy();
    this.headerResizeObserver?.disconnect();
    if (this.clickScrollTimeoutId !== null) {
      window.clearTimeout(this.clickScrollTimeoutId);
    }
  }

  operationSectionId(operationId: string): string {
    return getOperationSectionId(operationId);
  }

  selectOperation(operationId: string): void {
    const spec = this.view();
    const operation = spec?.operations.find(item => item.id === operationId);
    const docsRoot = this.docsRoot()?.nativeElement ?? this.host.nativeElement;
    const element = findOperationSectionElement(docsRoot, operationId);
    if (!operation || !element) {
      return;
    }

    if (this.clickScrollTimeoutId !== null) {
      window.clearTimeout(this.clickScrollTimeoutId);
    }

    this.isClickScrolling = true;
    this.applyOperationState(operation, { resetTryIt: true });

    const scrollContainer = getDocsScrollContainer(docsRoot);
    requestAnimationFrame(() => {
      scrollOperationSectionIntoView(element, scrollContainer);
    });

    this.clickScrollTimeoutId = window.setTimeout(() => {
      this.isClickScrolling = false;
      this.clickScrollTimeoutId = null;
    }, CLICK_SCROLL_LOCK_MS);
  }

  toggleSchemas(): void {
    this.schemasExpanded.update(value => !value);
  }

  updateEndpointSearch(value: string): void {
    this.endpointSearch.set(value);
  }

  updatePathValue(name: string, value: string): void {
    this.pathValues.update(current => ({ ...current, [name]: value }));
  }

  updateQueryValue(name: string, value: string): void {
    this.queryValues.update(current => ({ ...current, [name]: value }));
  }

  updateHeaderValue(name: string, value: string): void {
    this.headerValues.update(current => ({ ...current, [name]: value }));
  }

  updateBody(value: string): void {
    this.body.set(value);
  }

  updateServer(value: string): void {
    this.selectedServerUrl.set(value);
  }

  downloadSpec(): void {
    const spec = this.view();
    if (!spec) {
      return;
    }
    downloadSpecContent(spec.rawContent, spec.title);
  }

  async copyRequestUrl(): Promise<void> {
    const url = this.requestUrl();
    if (!url) {
      return;
    }
    await navigator.clipboard.writeText(url);
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 2000);
  }

  async copyCodeSample(): Promise<void> {
    await navigator.clipboard.writeText(this.codeSample());
  }

  async sendRequest(): Promise<void> {
    const operation = this.activeOperation();
    if (!operation) {
      return;
    }

    this.isSending.set(true);
    this.tryItError.set(null);
    try {
      const url = this.requestUrl();
      const headers = new Headers(this.headerValues());
      if (operation.hasRequestBody && this.body().trim()) {
        headers.set('Content-Type', 'application/json');
      }

      const startedAt = performance.now();
      const response = await fetch(url, {
        method: operation.method,
        headers,
        body: ['GET', 'HEAD'].includes(operation.method) ? undefined : this.body() || undefined,
      });
      const durationMs = Math.round(performance.now() - startedAt);
      const text = await response.text();
      this.tryItResponse.set({
        status: response.status,
        statusText: response.statusText,
        durationMs,
        body: text || '(empty response)',
        ok: response.ok,
      });
    } catch (error) {
      this.tryItResponse.set(null);
      this.tryItError.set(error instanceof Error ? error.message : 'Request failed');
    } finally {
      this.isSending.set(false);
    }
  }

  parametersFor(operation: GraviteeOperation, location: string) {
    return operation.parameters.filter(parameter => parameter.in === location);
  }

  private applyOperationState(operation: GraviteeOperation, options: { resetTryIt: boolean }): void {
    const alreadyActive = this.activeOperationId() === operation.id;
    this.activeOperationId.set(operation.id);

    if (!options.resetTryIt && alreadyActive) {
      return;
    }

    this.tryItError.set(null);
    this.tryItResponse.set(null);
    this.copied.set(false);

    const nextPath: Record<string, string> = {};
    const nextQuery: Record<string, string> = {};
    const nextHeader: Record<string, string> = {};
    operation.parameters.forEach(parameter => {
      const value = parameter.defaultValue ?? '';
      if (parameter.in === 'path') {
        nextPath[parameter.name] = value;
      } else if (parameter.in === 'query') {
        nextQuery[parameter.name] = value;
      } else if (parameter.in === 'header') {
        nextHeader[parameter.name] = value;
      }
    });
    this.pathValues.set(nextPath);
    this.queryValues.set(nextQuery);
    this.headerValues.set(nextHeader);
    this.body.set(operation.hasRequestBody ? '{\n  \n}' : '');
  }

  private setupHeaderOffsetObserver(): void {
    const docsRoot = this.docsRoot()?.nativeElement;
    const header = this.docsHeader()?.nativeElement;
    if (!docsRoot || !header) {
      return;
    }

    const updateOffset = () => {
      docsRoot.style.setProperty('--docs-sticky-header-offset', `${header.offsetHeight}px`);
    };
    updateOffset();
    this.headerResizeObserver?.disconnect();
    this.headerResizeObserver = new ResizeObserver(updateOffset);
    this.headerResizeObserver.observe(header);
  }

  private setupScrollSpy(): void {
    this.teardownScrollSpy();

    const docsRoot = this.docsRoot()?.nativeElement;
    const operations = this.view()?.operations ?? [];
    if (!docsRoot || operations.length === 0) {
      return;
    }

    const scrollContainer = getDocsScrollContainer(docsRoot);
    const headerOffset = getDocsStickyHeaderOffset(docsRoot);

    this.intersectionObserver = new IntersectionObserver(
      entries => {
        this.ngZone.run(() => {
          for (const entry of entries) {
            const operationId = entry.target.getAttribute('data-operation-id');
            if (!operationId) {
              continue;
            }
            if (entry.isIntersecting) {
              this.visibleSections.set(operationId, entry);
            } else {
              this.visibleSections.delete(operationId);
            }
          }

          if (this.isClickScrolling) {
            return;
          }

          const sorted = [...this.visibleSections.entries()].sort(
            (left, right) => left[1].boundingClientRect.top - right[1].boundingClientRect.top,
          );
          const nextId = sorted[0]?.[0];
          const nextOperation = operations.find(operation => operation.id === nextId);
          if (nextOperation && nextOperation.id !== this.activeOperationId()) {
            this.applyOperationState(nextOperation, { resetTryIt: true });
          }
        });
      },
      {
        root: scrollContainer,
        rootMargin: `-${Math.max(headerOffset, 1)}px 0px -55% 0px`,
        threshold: [0, 0.1, 0.25, 0.5, 0.75, 1],
      },
    );

    for (const operation of operations) {
      const element = findOperationSectionElement(docsRoot, operation.id);
      if (element) {
        this.intersectionObserver.observe(element);
      }
    }
  }

  private teardownScrollSpy(): void {
    this.intersectionObserver?.disconnect();
    this.intersectionObserver = undefined;
    this.visibleSections.clear();
  }
}
