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
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';

import { WORKSPACE_CALL_SNIPPET_FORMATS, WorkspaceCallSnippetFormatId, buildWorkspaceCallSnippet } from './workspace-call-snippets';
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
import { workspaceListBreadcrumb } from '../workspace-breadcrumbs';

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

  readonly formatBudget = formatAiWorkspaceBudget;
  readonly formatCost = formatAiWorkspaceCost;

  readonly keyColumns = ['status', 'key', 'createdAt'];
  readonly modelColumns = ['model', 'inputPrice', 'outputPrice'];

  readonly keysPageSize = 5;
  readonly keysPageSizeOptions = [5, 10, 20];
  readonly modelsPageSize = signal(5);
  readonly modelsPageSizeOptions = [5, 10, 20];
  readonly keysPage = signal(1);
  readonly modelsPage = signal(1);
  readonly selectedModelName = signal<string | null>(null);
  readonly selectedSnippetFormat = signal<WorkspaceCallSnippetFormatId>('curl');
  readonly snippetFormats = WORKSPACE_CALL_SNIPPET_FORMATS;
  readonly snippetCopied = signal(false);

  loading = signal(true);

  private readonly workspaceId$ = this.activatedRoute.paramMap.pipe(map(params => params.get('workspaceId') ?? ''));

  readonly workspace: Signal<AiWorkspace | null> = toSignal(
    this.workspaceId$.pipe(
      tap(() => {
        this.loading.set(true);
        this.selectedModelName.set(null);
        this.modelsPage.set(1);
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

  readonly consumption: Signal<AiWorkspaceConsumption> = toSignal(
    this.workspaceId$.pipe(switchMap(id => (id ? this.aiWorkspaceService.getConsumption(id) : of(this.emptyConsumption)))),
    { initialValue: this.emptyConsumption },
  );

  /** Full model list for the call-snippet picker. Paged below for the table, with no extra request. */
  readonly snippetModels = computed(() => (this.workspace()?.models ?? []).map(model => this.toDisplayModel(model)));

  readonly keys = computed(() => {
    const key = this.workspace()?.key;
    return key ? [key] : [];
  });

  readonly pagedKeys = computed(() => {
    const all = this.keys();
    const start = (this.keysPage() - 1) * this.keysPageSize;
    return all.slice(start, start + this.keysPageSize);
  });

  readonly models = computed(() => {
    const all = this.snippetModels();
    const start = (this.modelsPage() - 1) * this.modelsPageSize();
    return all.slice(start, start + this.modelsPageSize());
  });
  readonly modelsTotal = computed(() => this.snippetModels().length);
  readonly modelsCurrentPage = computed(() => this.modelsPage());

  readonly showCallSnippet = computed(() => !!this.workspace()?.endpointUrl);

  readonly codeSnippet = computed(() => {
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

  private toDisplayModel(model: NonNullable<AiWorkspace['models']>[number]): AiWorkspaceModel {
    return {
      name: model.name,
      inputPrice: model.inputPrice == null || model.inputPrice === '' ? undefined : String(model.inputPrice),
      outputPrice: model.outputPrice == null || model.outputPrice === '' ? undefined : String(model.outputPrice),
    };
  }
}
