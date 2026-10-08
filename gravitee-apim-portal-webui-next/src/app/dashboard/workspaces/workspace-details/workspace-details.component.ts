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
import { CdkCopyToClipboard } from '@angular/cdk/clipboard';
import { DatePipe } from '@angular/common';
import { Component, Signal, computed, effect, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';

import {
  WORKSPACE_CALL_SNIPPET_FORMATS,
  WorkspaceCallSnippetFormatId,
  buildWorkspaceCallSnippet,
} from './workspace-call-snippets';
import { workspaceListBreadcrumb } from '../workspace-breadcrumbs';
import { CopyCodeIconComponent } from '../../../../components/copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PaginationComponent } from '../../../../components/pagination/pagination.component';
import {
  AiWorkspace,
  AiWorkspaceConsumption,
  AiWorkspaceKey,
  AiWorkspaceModel,
  formatAiWorkspaceBudget,
  formatAiWorkspaceCost,
} from '../../../../entities/ai-workspace/ai-workspace';
import { AiWorkspaceService } from '../../../../services/ai-workspace.service';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';

@Component({
  selector: 'app-workspace-details',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatIcon,
    DatePipe,
    CdkCopyToClipboard,
    CopyCodeIconComponent,
    LoaderComponent,
    PaginationComponent,
  ],
  templateUrl: './workspace-details.component.html',
  styleUrl: './workspace-details.component.scss',
})
export default class WorkspaceDetailsComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly aiWorkspaceService = inject(AiWorkspaceService);
  private readonly breadcrumbService = inject(BreadcrumbService);

  protected readonly formatBudget = formatAiWorkspaceBudget;
  protected readonly formatCost = formatAiWorkspaceCost;

  protected readonly keyColumns = ['status', 'key', 'createdAt'];
  protected readonly modelColumns = ['model', 'inputPrice', 'outputPrice'];

  protected readonly keysPageSize = 5;
  protected readonly keysPageSizeOptions = [5, 10, 20];
  protected readonly modelsPageSize = signal(5);
  protected readonly modelsPageSizeOptions = [5, 10, 20];
  protected readonly keysPage = signal(1);
  protected readonly modelsPage = signal(1);
  protected readonly selectedModelName = signal<string | null>(null);
  protected readonly selectedSnippetFormat = signal<WorkspaceCallSnippetFormatId>('curl');
  protected readonly snippetFormats = WORKSPACE_CALL_SNIPPET_FORMATS;
  protected readonly snippetCopied = signal(false);

  loading = signal(true);

  private readonly workspaceId$ = this.activatedRoute.paramMap.pipe(map(params => params.get('workspaceId') ?? ''));

  protected readonly workspace: Signal<AiWorkspace | null> = toSignal(
    this.workspaceId$.pipe(
      tap(() => {
        this.loading.set(true);
        this.selectedModelName.set(null);
      }),
      switchMap(id =>
        id
          ? this.aiWorkspaceService.get(id).pipe(
              catchError(() => {
                this.router.navigate(['/dashboard', 'workspaces']);
                return of(null);
              }),
            )
          : of(null),
      ),
      tap(workspace => {
        if (workspace) {
          this.breadcrumbService.set([workspaceListBreadcrumb(true), { id: `workspace-${workspace.id}`, label: workspace.name }]);
        }
        this.loading.set(false);
      }),
    ),
    { initialValue: null },
  );

  private readonly emptyConsumption: AiWorkspaceConsumption = { tokens: 0, requests: 0, cost: 0, from: '', to: '' };

  protected readonly consumption: Signal<AiWorkspaceConsumption> = toSignal(
    this.workspaceId$.pipe(switchMap(id => (id ? this.aiWorkspaceService.getConsumption(id) : of(this.emptyConsumption)))),
    { initialValue: this.emptyConsumption },
  );

  private readonly modelsResponse = toSignal(
    combineLatest([this.workspaceId$, toObservable(this.modelsPage), toObservable(this.modelsPageSize)]).pipe(
      switchMap(([id, page, size]) =>
        id
          ? this.aiWorkspaceService.listModels(id, page, size)
          : of({ data: [] as AiWorkspaceModel[], metadata: { pagination: { current_page: 1, size: 5, total: 0 } } }),
      ),
    ),
    { initialValue: { data: [] as AiWorkspaceModel[], metadata: { pagination: { current_page: 1, size: 5, total: 0 } } } },
  );

  /** Full model list for the call-snippet picker (not limited to the table page). */
  protected readonly snippetModels: Signal<AiWorkspaceModel[]> = toSignal(
    this.workspaceId$.pipe(
      switchMap(id => (id ? this.aiWorkspaceService.listModels(id, 1, 100).pipe(map(response => response.data ?? [])) : of([]))),
    ),
    { initialValue: [] },
  );

  protected readonly keys = computed(() => {
    const key = this.workspace()?.key;
    return key ? [key] : [];
  });

  protected readonly pagedKeys = computed(() => {
    const all = this.keys();
    const start = (this.keysPage() - 1) * this.keysPageSize;
    return all.slice(start, start + this.keysPageSize);
  });

  protected readonly models = computed(() => this.modelsResponse().data ?? []);
  protected readonly modelsTotal = computed(() => this.modelsResponse().metadata?.pagination?.total ?? this.models().length);
  protected readonly modelsCurrentPage = computed(() => this.modelsResponse().metadata?.pagination?.current_page ?? this.modelsPage());

  protected readonly showCallSnippet = computed(() => !!this.workspace()?.endpointUrl);

  protected readonly codeSnippet = computed(() => {
    const ws = this.workspace();
    const modelName = this.selectedModelName();
    if (!ws?.endpointUrl || !modelName) {
      return '';
    }
    return buildWorkspaceCallSnippet(this.selectedSnippetFormat(), {
      endpointUrl: ws.endpointUrl,
      apiKey: ws.key?.value ?? '',
      modelName,
    });
  });

  constructor() {
    effect(() => {
      const models = this.snippetModels();
      const selected = this.selectedModelName();
      if (!models.length) {
        if (selected !== null) {
          this.selectedModelName.set(null);
        }
        return;
      }
      if (!selected || !models.some(model => model.name === selected)) {
        this.selectedModelName.set(models[0].name);
      }
    });
  }

  onKeysPageChange(page: number) {
    this.keysPage.set(page);
  }

  onModelsPageChange(page: number) {
    this.modelsPage.set(page);
  }

  onModelsPageSizeChange(size: number) {
    this.modelsPageSize.set(size);
    this.modelsPage.set(1);
  }

  onSnippetModelChange(modelName: string) {
    this.selectedModelName.set(modelName);
  }

  onSnippetFormatChange(format: WorkspaceCallSnippetFormatId) {
    this.selectedSnippetFormat.set(format);
  }

  onSnippetCopied() {
    this.snippetCopied.set(true);
    setTimeout(() => this.snippetCopied.set(false), 2000);
  }

  keyStatusIcon(key: AiWorkspaceKey): string {
    return key.status === 'ACTIVE' ? 'check_circle' : 'pause_circle';
  }
}
