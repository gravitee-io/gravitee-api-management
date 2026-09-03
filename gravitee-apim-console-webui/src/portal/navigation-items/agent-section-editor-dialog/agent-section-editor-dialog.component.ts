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
import { AsyncPipe } from '@angular/common';
import { Component, computed, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxChange, MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { isEqual } from 'lodash';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';

import { Api, ApisResponse, PortalNavigationItem, PortalVisibility } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { getPublicVisibilityDisabledTooltip, isPublicVisibilityDisabled } from '../visibility-toggle.util';

export interface AgentSectionEditorDialogData {
  mode: 'create';
  parentItem: PortalNavigationItem;
  existingAgentIds?: string[];
}

export interface SelectedAgent {
  id: string;
  name: string;
}

export interface AgentSectionEditorDialogResult {
  visibility: PortalVisibility;
  agents: SelectedAgent[];
}

type AgentRow = SelectedAgent & {
  version: string;
  description: string;
  isDisabled: boolean;
};

interface AgentSectionFormControls {
  agentIds: FormControl<string[]>;
  isPrivate: FormControl<boolean>;
}

interface AgentSectionFormValues {
  agentIds: string[];
  isPrivate: boolean;
}

type AgentSectionForm = FormGroup<AgentSectionFormControls>;

@Component({
  selector: 'agent-section-editor-dialog',
  imports: [
    MatDialogModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatIconModule,
    MatCheckboxModule,
    MatTableModule,
    MatChipsModule,
    MatExpansionModule,
    AsyncPipe,
    GioTableWrapperModule,
  ],
  templateUrl: './agent-section-editor-dialog.component.html',
  styleUrls: ['./agent-section-editor-dialog.component.scss'],
})
export class AgentSectionEditorDialogComponent implements OnInit {
  private readonly apiService = inject(ApiV2Service);
  private readonly dialogData = inject<AgentSectionEditorDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<AgentSectionEditorDialogComponent, AgentSectionEditorDialogResult>);

  form!: AgentSectionForm;
  initialFormValues!: AgentSectionFormValues;
  readonly title = 'Add Agents';
  readonly displayedColumns = ['select', 'name', 'version', 'description'];
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  total = 0;
  readonly isLoading = signal(true);
  readonly hasLoadError = signal(false);
  readonly selectedPanelExpanded = signal(true);

  private readonly filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private readonly selectedOrderedAgents = signal<SelectedAgent[]>([]);

  readonly selectedCount = computed(() => this.selectedOrderedAgents().length);
  readonly selectedAgents = computed(() => this.selectedOrderedAgents());
  readonly publicDisabled = computed(() => isPublicVisibilityDisabled(this.dialogData.parentItem));
  readonly publicDisabledTooltip = computed(() => getPublicVisibilityDisabledTooltip(this.dialogData.parentItem));

  rows$!: Observable<AgentRow[]>;

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent): void {
    if (!this.formIsUnchanged()) {
      event.preventDefault();
    }
  }

  ngOnInit(): void {
    this.form = new FormGroup<AgentSectionFormControls>({
      agentIds: new FormControl<string[]>([], {
        validators: [Validators.required],
        nonNullable: true,
      }),
      isPrivate: new FormControl<boolean>(false, {
        nonNullable: true,
      }),
    });

    const disabledAgentIds = new Set(this.dialogData.existingAgentIds ?? []);

    this.rows$ = this.filters$.pipe(
      debounceTime(100),
      distinctUntilChanged(isEqual),
      tap(() => {
        this.isLoading.set(true);
        this.hasLoadError.set(false);
      }),
      switchMap(filters =>
        this.apiService
          .search(
            { query: filters.searchTerm, apiTypes: ['V4_A2A_PROXY'] },
            undefined,
            filters.pagination.index,
            filters.pagination.size,
            false,
          )
          .pipe(
            catchError((): Observable<ApisResponse> => {
              this.hasLoadError.set(true);
              return of({ data: [], pagination: undefined, links: undefined });
            }),
          ),
      ),
      tap(response => {
        this.isLoading.set(false);
        this.total = response.pagination?.totalCount ?? 0;
      }),
      map(response =>
        (response.data ?? []).map((api: Api) => ({
          id: api.id,
          name: api.name,
          version: api.apiVersion ?? '',
          description: api.description ?? '',
          isDisabled: disabledAgentIds.has(api.id),
        })),
      ),
    );

    this.syncVisibilityControlState();
    this.initialFormValues = this.form.getRawValue();
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  isChecked(agentId: string): boolean {
    return this.form.controls.agentIds.value.includes(agentId);
  }

  onAgentSelectionChange(agent: SelectedAgent, event: MatCheckboxChange): void {
    if (event.checked) {
      this.addAgent({ id: agent.id, name: agent.name });
      return;
    }

    this.removeAgent(agent.id);
  }

  removeSelected(agentId: string): void {
    this.removeAgent(agentId);
  }

  onSubmit(): void {
    if (!this.form.valid) {
      return;
    }

    const formValues = this.form.getRawValue();
    this.dialogRef.close({
      visibility: formValues.isPrivate ? 'PRIVATE' : 'PUBLIC',
      agents: this.selectedOrderedAgents(),
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  formIsUnchanged(): boolean {
    return isEqual(this.form.getRawValue(), this.initialFormValues);
  }

  private addAgent(agent: SelectedAgent): void {
    const currentAgentIds = this.form.controls.agentIds.value;
    if (currentAgentIds.includes(agent.id)) {
      return;
    }

    this.form.controls.agentIds.setValue([...currentAgentIds, agent.id]);
    this.selectedOrderedAgents.update(selectedAgents => [...selectedAgents, agent]);
  }

  private removeAgent(agentId: string): void {
    const currentAgentIds = this.form.controls.agentIds.value;
    if (!currentAgentIds.includes(agentId)) {
      return;
    }

    this.form.controls.agentIds.setValue(currentAgentIds.filter(id => id !== agentId));
    this.selectedOrderedAgents.update(selectedAgents => selectedAgents.filter(agent => agent.id !== agentId));
  }

  private syncVisibilityControlState(): void {
    const isPrivateControl = this.form.controls.isPrivate;

    if (this.publicDisabled()) {
      isPrivateControl.setValue(true, { emitEvent: false });
      isPrivateControl.disable({ emitEvent: false });
      return;
    }

    isPrivateControl.enable({ emitEvent: false });
  }
}
