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
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';

import { PolicyService } from '../../../services-ngx/policy.service';
import { FlowService } from '../../../services-ngx/flow.service';
import { OrganizationService } from '../../../services-ngx/organization.service';
import { Organization } from '../../../entities/organization/organization';
import { PlatformFlowSchema } from '../../../entities/flow/platformFlowSchema';
import { PolicyListItem } from '../../../entities/policy';
import { PathOperator, Step } from '../../../entities/flow/flow';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '../../../shared/components/gio-confirm-dialog/gio-confirm-dialog.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

interface FlowVM {
  name: string;
  'path-operator': PathOperator;
  pre: Step[];
  post: Step[];
  enabled: boolean;
  methods: string[];
  condition: string;
  consumers: string[];
}

interface DefinitionVM {
  'flow-mode': 'DEFAULT' | 'BEST_MATCH';
  flows: FlowVM[];
}

@Component({
  selector: 'org-settings-platform-policies',
  template: require('./org-settings-platform-policies.component.html'),
  styles: [require('./org-settings-platform-policies.component.scss')],
})
export class OrgSettingsPlatformPoliciesComponent implements OnInit, OnDestroy {
  isLoading = true;

  organization: Organization;
  definition: DefinitionVM;

  platformFlowSchema: PlatformFlowSchema;
  policies: PolicyListItem[];

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly flowService: FlowService,
    private readonly policyService: PolicyService,
    private readonly organizationService: OrganizationService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.flowService.getPlatformFlowSchemaForm(),
      this.policyService.list({ expandSchema: true, expandIcon: true, withoutResource: true }),
      this.organizationService.get(),
    ])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([flowSchema, policies, organization]) => {
          this.platformFlowSchema = flowSchema;
          this.policies = policies;
          this.organization = organization;
          this.definition = {
            flows: (this.organization.flows ?? []).map((flow) => ({
              ...flow,
              consumers: flow.consumers.map((consumer) => consumer.consumerId),
            })),
            'flow-mode': this.organization.flowMode,
          };
          this.isLoading = false;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSave({ definition }: { definition: DefinitionVM }) {
    const updatedOrganization: Organization = {
      ...this.organization,
      flowMode: definition['flow-mode'],
      flows: definition.flows.map((flow) => ({
        ...flow,
        consumers: (flow.consumers ?? []).map((consumer) => ({ consumerType: 'TAG', consumerId: consumer })),
      })),
    };

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
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.organizationService.update(updatedOrganization)),
        tap(() => {
          this.snackBarService.success('Platform policies successfully updated!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe(() => {
        this.definition = { ...this.definition };
      });
  }
}
