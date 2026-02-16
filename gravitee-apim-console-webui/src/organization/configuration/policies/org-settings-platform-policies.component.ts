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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { OrganizationService } from '../../../services-ngx/organization.service';
import { Organization } from '../../../entities/organization/organization';
import { PathOperator, Step } from '../../../entities/flow/flow';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

interface FlowVM {
  name?: string;
  'path-operator': PathOperator;
  pre: Step[];
  post: Step[];
  enabled: boolean;
  methods?: string[];
  condition?: string;
  consumers?: string[];
}

export interface DefinitionVM {
  flow_mode?: 'DEFAULT' | 'BEST_MATCH';
  flows?: FlowVM[];
}

@Component({
  selector: 'org-settings-platform-policies',
  templateUrl: './org-settings-platform-policies.component.html',
  styleUrls: ['./org-settings-platform-policies.component.scss'],
  standalone: false,
})
export class OrgSettingsPlatformPoliciesComponent implements OnInit, OnDestroy {
  isLoading = true;
  activeTab: 'design' | 'config' = 'design';

  definitionToSave: DefinitionVM;

  isSaveButtonDisabled = true;

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly organizationService: OrganizationService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.isLoading = false;
    this.isSaveButtonDisabled = true;
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onChangeStudio(flows: DefinitionVM['flows']) {
    this.definitionToSave = {
      ...this.definitionToSave,
      flows,
    };
    this.isSaveButtonDisabled = false;
  }

  onConfigChange(flow_mode: DefinitionVM['flow_mode']) {
    this.definitionToSave = {
      ...this.definitionToSave,
      flow_mode,
    };
    this.isSaveButtonDisabled = false;
  }

  onSave() {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '450px',
        data: {
          title: 'Deploy the policies?',
          content: 'Platform policies will be automatically deployed on gateways.',
        },
        role: 'alertdialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        tap(() => {
          this.isLoading = true;
          this.isSaveButtonDisabled = true;
        }),
        switchMap(() => this.organizationService.get()),
        map(
          organization =>
            ({
              ...organization,
              flowMode: this.definitionToSave.flow_mode ?? organization.flowMode,
              flows:
                this.definitionToSave.flows?.map(flow => ({
                  ...flow,
                  consumers: (flow.consumers ?? []).map(consumer => ({ consumerType: 'TAG', consumerId: consumer })),
                })) ?? organization.flows,
            }) as Organization,
        ),
        switchMap(organizationToSave => this.organizationService.update(organizationToSave)),
        tap(() => {
          this.snackBarService.success('Platform policies successfully updated!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.ngOnInit();
      });
  }
}
