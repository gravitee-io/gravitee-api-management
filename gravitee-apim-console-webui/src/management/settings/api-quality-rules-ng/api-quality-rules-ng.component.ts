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
import { Component, Inject, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { FormControl, FormGroup, UntypedFormGroup } from '@angular/forms';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import {
  ApiQualityRulesDialogData,
  ApiQualityRulesDialogResult,
  ApiQualityRulesNgAddDialogComponent,
} from './api-quality-rules-ng-add-dialog/api-quality-rules-ng-add-dialog.component';

import { Constants, EnvSettings } from '../../../entities/Constants';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { QualityRuleService } from '../../../services-ngx/quality-rule.service';
import { QualityRule } from '../../../entities/qualityRule';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

interface ApiQualityRulesForm {
  apiReview: FormGroup<{
    enabled: FormControl<boolean>;
  }>;
  apiQualityMetrics: FormGroup<{
    enabled: FormControl<boolean>;
    descriptionWeight: FormControl<number>;
    descriptionMinLength: FormControl<number>;
    logoWeight: FormControl<number>;
    categoriesWeight: FormControl<number>;
    labelsWeight: FormControl<number>;
    functionalDocumentationWeight: FormControl<number>;
    technicalDocumentationWeight: FormControl<number>;
    healthcheckWeight: FormControl<number>;
  }>;
}

@Component({
  selector: 'api-quality-rules-ng',
  templateUrl: './api-quality-rules-ng.component.html',
  styleUrls: ['./api-quality-rules-ng.component.scss'],
})
export class ApiQualityRulesNgComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  settings: EnvSettings;
  apiQualityRulesForm: UntypedFormGroup;
  public formInitialValues: unknown;
  public qualityRulesListTable: QualityRule[] = [];
  public displayedColumns = ['quality', 'description', 'weight', 'actions'];
  public qualityRulesUnpaginatedLength = 0;
  public filteredQualityRulesSettingsTable: QualityRule[] = [];
  public isLoadingData = true;
  public initialFilters: GioTableWrapperFilters = {
    pagination: {
      size: 10,
      index: 1,
    },
    searchTerm: '',
  };

  constructor(
    @Inject('Constants') private readonly constants: Constants,
    private readonly portalSettingsService: PortalSettingsService,
    private readonly snackBarService: SnackBarService,
    private readonly environmentSettingsService: EnvironmentSettingsService,
    private readonly qualityRuleService: QualityRuleService,
    private readonly matDialog: MatDialog,
  ) {}

  public ngOnInit() {
    this.isLoadingData = true;
    this.settings = this.constants.env.settings;
    this.qualityRuleService
      .list()
      .pipe(
        tap((qualityRulesList) => {
          this.qualityRulesListTable = qualityRulesList;
          this.onPropertiesFiltersChanged(this.initialFilters);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
      });

    this.apiQualityRulesForm = new FormGroup<ApiQualityRulesForm>({
      apiReview: new FormGroup({
        enabled: new FormControl({
          value: this.settings.apiReview.enabled,
          disabled: this.isReadonly('api.review.enabled'),
        }),
      }),
      apiQualityMetrics: new FormGroup({
        enabled: new FormControl({
          value: this.settings.apiQualityMetrics.enabled,
          disabled: this.isReadonly('api.quality.metrics.enabled'),
        }),
        descriptionWeight: new FormControl({
          value: this.settings.apiQualityMetrics.descriptionWeight,
          disabled: this.isReadonly('api.quality.metrics.description.weight'),
        }),
        descriptionMinLength: new FormControl({
          value: this.settings.apiQualityMetrics.descriptionMinLength,
          disabled: this.isReadonly('api.quality.metrics.description.min.length'),
        }),
        logoWeight: new FormControl({
          value: this.settings.apiQualityMetrics.logoWeight,
          disabled: this.isReadonly('api.quality.metrics.logo.weight'),
        }),
        categoriesWeight: new FormControl({
          value: this.settings.apiQualityMetrics.categoriesWeight,
          disabled: this.isReadonly('api.quality.metrics.categories.weight'),
        }),
        labelsWeight: new FormControl({
          value: this.settings.apiQualityMetrics.labelsWeight,
          disabled: this.isReadonly('api.quality.metrics.labels.weight'),
        }),
        functionalDocumentationWeight: new FormControl({
          value: this.settings.apiQualityMetrics.functionalDocumentationWeight,
          disabled: this.isReadonly('api.quality.metrics.functional.documentation.weight'),
        }),
        technicalDocumentationWeight: new FormControl({
          value: this.settings.apiQualityMetrics.technicalDocumentationWeight,
          disabled: this.isReadonly('api.quality.metrics.technical.documentation.weight'),
        }),
        healthcheckWeight: new FormControl({
          value: this.settings.apiQualityMetrics.healthcheckWeight,
          disabled: this.isReadonly('api.quality.metrics.healthcheck.weight'),
        }),
      }),
    });

    this.formInitialValues = this.apiQualityRulesForm.getRawValue();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  isReadonly(property: string): boolean {
    return PortalSettingsService.isReadonly(this.settings, property);
  }

  onSubmit() {
    const updatedSettings = {
      ...this.settings,
      ...this.apiQualityRulesForm.getRawValue(),
    };

    this.portalSettingsService
      .save(updatedSettings)
      .pipe(
        switchMap(() => this.environmentSettingsService.get()),
        tap(() => this.snackBarService.success('API Quality details successfully updated!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  deleteRule(name: string, id: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete manual rule',
          content: `Are you sure you want to delete manual rule <strong>${name}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteManualRule',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.qualityRuleService.delete(id)),
        tap(() => {
          this.snackBarService.success(`“${name}” has been deleted”`);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  addQualityRule() {
    this.matDialog
      .open<ApiQualityRulesNgAddDialogComponent, ApiQualityRulesDialogData, ApiQualityRulesDialogResult>(
        ApiQualityRulesNgAddDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          role: 'dialog',
          id: 'addQualityRule',
        },
      )
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((newQualityRule) => this.qualityRuleService.add(newQualityRule)),
        tap(() => {
          this.snackBarService.success('New quality rule created successfully');
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  updateRule(element: ApiQualityRulesDialogData) {
    this.matDialog
      .open<ApiQualityRulesNgAddDialogComponent, ApiQualityRulesDialogData, ApiQualityRulesDialogResult>(
        ApiQualityRulesNgAddDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          role: 'dialog',
          id: 'updateQualityRule',
          data: {
            ...element,
          },
        },
      )
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((editedQualityRule) => this.qualityRuleService.update(element.id, editedQualityRule)),
        tap(() => {
          this.snackBarService.success('Quality rule updated successfully');
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onPropertiesFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.qualityRulesListTable, filters);
    this.filteredQualityRulesSettingsTable = filtered.filteredCollection;
    this.qualityRulesUnpaginatedLength = filtered.unpaginatedLength;
  }
}
