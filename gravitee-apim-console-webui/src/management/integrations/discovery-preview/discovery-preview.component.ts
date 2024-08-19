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

import { Component, DestroyRef, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup } from '@angular/forms';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { IntegrationPreview, IntegrationPreviewApisState } from '../integrations.model';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

@Component({
  selector: 'app-discovery-preview',
  templateUrl: './discovery-preview.component.html',
  styleUrls: ['./discovery-preview.component.scss'],
})
export class DiscoveryPreviewComponent {
  IntegrationPreviewApisState = IntegrationPreviewApisState;
  private destroyRef: DestroyRef = inject(DestroyRef);

  private selectToIngest = new Set<IntegrationPreviewApisState>([IntegrationPreviewApisState.NEW, IntegrationPreviewApisState.UPDATE]);

  public displayedColumns = ['name', 'state'];
  public isLoadingPreview = true;
  public integrationPreview: IntegrationPreview = null;
  public ingestParametersForm: FormGroup<Record<IntegrationPreviewApisState, FormControl<boolean | null>>> = new FormGroup({
    NEW: new FormControl(false),
    UPDATE: new FormControl(false),
  });
  public nbTotalInstances = -1;
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
    sort: { active: 'name', direction: 'asc' },
  };
  public apisFiltered: IntegrationPreview['apis'] = [];

  private integrationId = this.activatedRoute.snapshot.params.integrationId;

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngAfterViewInit() {
    this.getIntegration();
    this.getPreview();
  }

  private getIntegration(): void {
    this.integrationsService
      .getIntegration(this.integrationId)
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          this.router.navigate(['../..'], {
            relativeTo: this.activatedRoute,
          });
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private getPreview(): void {
    const { integrationId } = this.activatedRoute.snapshot.params;

    this.integrationsService
      .previewIntegration(integrationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (integrationPreview) => {
          this.integrationPreview = integrationPreview;
          this.nbTotalInstances = integrationPreview.totalCount;
          this.apisFiltered = this.integrationPreview.apis;
          this.setupForm(IntegrationPreviewApisState.NEW, this.integrationPreview.newCount);
          this.setupForm(IntegrationPreviewApisState.UPDATE, this.integrationPreview.updateCount);
          this.runFilters(this.filters);
          this.isLoadingPreview = false;
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

  public apiToIngest(): IntegrationPreview['apis'] {
    return this.integrationPreview?.apis?.filter((api) => this.selectToIngest.has(api.state)) ?? [];
  }

  private setupForm(controlName: IntegrationPreviewApisState, value: number) {
    if (value <= 0) {
      this.ingestParametersForm.controls[controlName].disable({ onlySelf: true });
      this.selectToIngest.delete(controlName);
    } else {
      this.ingestParametersForm.controls[controlName].setValue(value > 0);
      this.selectToIngest.add(controlName);
    }
    this.ingestParametersForm.controls[controlName].valueChanges.subscribe((selected) => {
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
