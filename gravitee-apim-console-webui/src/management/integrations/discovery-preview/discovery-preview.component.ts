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
import { catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup } from '@angular/forms';
import { MatTableDataSource } from '@angular/material/table';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { IntegrationPreview, IntegrationPreviewApis, IntegrationPreviewApisState } from '../integrations.model';
import { fieldIsSet, fieldSet, fieldUnSet } from '../../../shared/utils';

@Component({
  selector: 'app-discovery-preview',
  templateUrl: './discovery-preview.component.html',
  styleUrls: ['./discovery-preview.component.scss'],
})
export class DiscoveryPreviewComponent implements OnInit {
  IntegrationPreviewApisState = IntegrationPreviewApisState;
  private destroyRef: DestroyRef = inject(DestroyRef);

  private static readonly NEW_BITFIELD_VALUE = 0b01;
  private static readonly UPDATE_BITFIELD_VALUE = 0b10;

  public displayedColumns = ['name', 'state'];
  public isLoadingPreview = true;
  public integrationPreview: IntegrationPreview = null;
  public ingestParametersForm = new FormGroup({
    ingestNewApis: new FormControl(false),
    ingestUpdateApis: new FormControl(false),
  });
  public tableData = new MatTableDataSource<IntegrationPreviewApis>();

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.getIntegration();
    this.getPreview();
  }

  private getIntegration(): void {
    const { integrationId } = this.activatedRoute.snapshot.params;

    this.integrationsService
      .getIntegration(integrationId)
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
          this.tableData.data = this.integrationPreview.apis;
          this.tableData.filterPredicate = (api, filter) => {
            const filterValues = parseInt(filter, 10);
            return (
              (fieldIsSet(filterValues, DiscoveryPreviewComponent.NEW_BITFIELD_VALUE) && api.state === IntegrationPreviewApisState.NEW) ||
              (fieldIsSet(filterValues, DiscoveryPreviewComponent.UPDATE_BITFIELD_VALUE) &&
                api.state === IntegrationPreviewApisState.UPDATE)
            );
          };
          this.setupForm('ingestNewApis', this.integrationPreview.newCount);
          this.setupForm('ingestUpdateApis', this.integrationPreview.updateCount);
          this.isLoadingPreview = false;
        },
      });
  }

  public proceedIngest() {
    this.integrationsService.prepareRunIngest(this.tableData.filteredData.map((api) => api.id));
    this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }

  private setupForm(controlName: 'ingestUpdateApis' | 'ingestNewApis', value: number) {
    if (value <= 0) {
      this.ingestParametersForm.controls[controlName].disable({ onlySelf: true });
    } else {
      this.ingestParametersForm.controls[controlName].setValue(value > 0);
    }
    const state =
      controlName === 'ingestNewApis' ? DiscoveryPreviewComponent.NEW_BITFIELD_VALUE : DiscoveryPreviewComponent.UPDATE_BITFIELD_VALUE;
    this.ingestParametersForm.controls[controlName].valueChanges.subscribe((selected) => {
      this.tableData.filter = (selected ? fieldSet(this.tableData.filter, state) : fieldUnSet(this.tableData.filter, state)).toString();
    });
  }
}
