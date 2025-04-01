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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { switchMap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup } from '@angular/forms';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { AgentStatus, IntegrationPreview, IntegrationPreviewApi, IntegrationPreviewApisState } from '../integrations.model';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

@Component({
  selector: 'app-discovery-preview',
  templateUrl: './discovery-preview.component.html',
  styleUrls: ['./discovery-preview.component.scss'],
  standalone: false,
})
export class DiscoveryPreviewComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading = true;
  public readonly IntegrationPreviewApisState = IntegrationPreviewApisState;
  private integrationId = this.activatedRoute.snapshot.params.integrationId;

  private selectToIngest = new Set<IntegrationPreviewApisState>([IntegrationPreviewApisState.NEW, IntegrationPreviewApisState.UPDATE]);
  public form: FormGroup<Record<IntegrationPreviewApisState, FormControl<boolean | null>>> = new FormGroup({
    NEW: new FormControl(false),
    UPDATE: new FormControl(false),
  });

  public displayedColumns = ['name', 'state'];
  public integrationPreview: IntegrationPreview = null;
  public nbTotalInstances = -1;

  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
    sort: { active: null, direction: null },
  };

  public apisFiltered: IntegrationPreviewApi[] = [];

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.getIntegration();
  }

  private getIntegration(): void {
    this.integrationsService
      .getIntegration(this.integrationId)
      .pipe(
        switchMap((integration) => {
          if (integration.agentStatus === AgentStatus.DISCONNECTED) {
            this.snackBarService.error('Agent is DISCONNECTED, make sure your Agent is CONNECTED');
            this.router.navigate(['..'], { relativeTo: this.activatedRoute });
            return EMPTY;
          } else {
            return this.integrationsService.previewIntegration(this.integrationId);
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (integrationPreview: IntegrationPreview) => {
          this.nbTotalInstances = integrationPreview.totalCount;
          this.apisFiltered = integrationPreview.apis;
          this.integrationPreview = integrationPreview;
          this.setupForm(IntegrationPreviewApisState.NEW, this.integrationPreview.newCount);
          this.setupForm(IntegrationPreviewApisState.UPDATE, this.integrationPreview.updateCount);
          this.runFilters(this.filters);
          this.isLoading = false;
        },
      });
  }

  public cancel() {
    return this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }

  public proceedIngest() {
    this.integrationsService
      .ingest(
        this.integrationId,
        this.apiToIngest().map((api) => api.id),
      )
      .subscribe((response) => {
        switch (response.status) {
          case 'SUCCESS':
            this.snackBarService.success('Ingestion complete! Your integration is now updated.');
            break;
          case 'ERROR':
            this.snackBarService.error(`Ingestion failed. Please check your settings and try again: ${response.message}`);
            break;
        }
        this.router.navigate(['..'], { relativeTo: this.activatedRoute });
      });
  }

  public apiToIngest(): IntegrationPreviewApi[] {
    return this.integrationPreview?.apis?.filter((api) => this.selectToIngest.has(api.state)) ?? [];
  }

  private setupForm(controlName: IntegrationPreviewApisState, value: number) {
    if (value <= 0) {
      this.form.controls[controlName].disable({ onlySelf: true });
      this.selectToIngest.delete(controlName);
    } else {
      this.form.controls[controlName].setValue(value > 0);
      this.selectToIngest.add(controlName);
    }
    this.form.controls[controlName].valueChanges.subscribe((selected) => {
      if (selected) {
        this.selectToIngest.add(controlName);
      } else {
        this.selectToIngest.delete(controlName);
      }
      this.runFilters(this.filters);
    });
  }

  public runFilters(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    const filtered = gioTableFilterCollection(this.apiToIngest(), filters);
    this.apisFiltered = filtered.filteredCollection;
    this.nbTotalInstances = filtered.unpaginatedLength;
  }
}
