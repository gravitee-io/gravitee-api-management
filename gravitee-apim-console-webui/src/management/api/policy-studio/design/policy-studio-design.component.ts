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
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { Location } from '@angular/common';
import '@gravitee/ui-components/wc/gv-design';
import { action } from '@storybook/addon-actions';
import { MatDialog } from '@angular/material/dialog';

import { PolicyStudioDesignService } from './policy-studio-design.service';
import { ChangeDesignEvent } from './models/ChangeDesignEvent';

import { Organization } from '../../../../entities/organization/organization';
import { PolicyListItem } from '../../../../entities/policy';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ResourceListItem } from '../../../../entities/resource/resourceListItem';
import { Services } from '../../../../entities/services';
import { FlowSchema } from '../../../../entities/flow/flowSchema';
import { Grammar } from '../../../../entities/spel/grammar';
import { ApiDefinition } from '../models/ApiDefinition';
import { PolicyStudioService } from '../policy-studio.service';
import {
  GioEeUnlockDialogComponent,
  GioEeUnlockDialogData,
} from '../../../../components/gio-ee-unlock-dialog/gio-ee-unlock-dialog.component';
import { GioLicenseService } from '../../../../shared/components/gio-license/gio-license.service';

export interface UrlParams {
  path: string;
  flowsIds: string[];
}

@Component({
  selector: 'policy-studio-design',
  template: require('./policy-studio-design.component.html'),
  styles: [require('./policy-studio-design.component.scss')],
})
export class PolicyStudioDesignComponent implements OnInit, OnDestroy {
  organization: Organization;
  apiDefinition: ApiDefinition;
  isReadonly = false;
  services: Services;

  apiFlowSchema: FlowSchema;
  policies: PolicyListItem[];
  resourceTypes: ResourceListItem[];
  readonlyPlans = false;
  policyDocumentation!: { id: string; image: string; content: string };
  selectedFlowsIds: Array<string> = [];

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly location: Location,
    private readonly policyStudioService: PolicyStudioService,
    private readonly policyStudioDesignService: PolicyStudioDesignService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
    private readonly licenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.policyStudioDesignService.getFlowSchemaForm(),
      this.policyStudioDesignService.listPolicies({ expandSchema: true, expandIcon: true }),
      this.policyStudioService.getApiDefinition$(),
      this.policyStudioDesignService.listResources({ expandSchema: true, expandIcon: true }),
    ])
      .pipe(
        tap(([flowSchema, policies, definition, resourceTypes]) => {
          this.apiFlowSchema = flowSchema;
          this.policies = policies;
          this.apiDefinition = definition;
          this.isReadonly = definition.origin === 'kubernetes';
          this.resourceTypes = resourceTypes;
          if (!this.permissionService.hasAnyMatching(['api-plan-u'])) {
            this.readonlyPlans = true;
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    const { flowsIds } = this.parseUrl();
    this.selectedFlowsIds = [JSON.stringify(flowsIds)];
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  async onChange($event: ChangeDesignEvent) {
    const { isDirty, errors, definition } = $event.detail;
    if (isDirty && errors === 0 && definition != null) {
      this.apiDefinition = definition;
      this.policyStudioService.saveApiDefinition(this.apiDefinition);
    }
  }

  public onFlowSelectionChanged({ flows }: { flows: string[] }): void {
    this.updateUrl({ ...this.parseUrl(), flowsIds: flows });
  }

  public displayPolicyCta() {
    const featureMoreInformation = this.licenseService.getFeatureMoreInformation('apim-policy');
    this.matDialog
      .open<GioEeUnlockDialogComponent, GioEeUnlockDialogData, boolean>(GioEeUnlockDialogComponent, {
        data: {
          featureMoreInformation,
        },
        role: 'alertdialog',
        id: 'gioLicenseDialog',
      })
      .afterClosed()
      .pipe(
        tap((confirmed) => {
          action('confirmed?')(confirmed);
        }),
      )
      .subscribe();
  }

  public fetchPolicyDocumentation({ policy }: { policy: { id: string; icon: string } }): void {
    this.policyStudioDesignService
      .getDocumentation(policy.id)
      .pipe(
        tap(
          (documentation) =>
            (this.policyDocumentation = {
              id: policy.id,
              image: policy.icon,
              content: documentation,
            }),
        ),
      )
      .subscribe();
  }

  public fetchSpelGrammar({ currentTarget }: { currentTarget: { grammar: Grammar } }): void {
    this.policyStudioDesignService
      .getSpelGrammar()
      .pipe(tap((grammar) => (currentTarget.grammar = grammar)))
      .subscribe();
  }

  private parseUrl(): UrlParams {
    // TODO: Improve this with Angular Router
    // Hack to add the tab as Fragment part of the URL
    const [path] = this.location.path(true).split(/#(\w*)$/);

    const [basePath, ...flowsIds] = path.split('flows');

    const cleanedPath = basePath.replace('?', '');
    const cleanedFlows = (flowsIds ?? []).map((flow) => flow.replace('=', ''));

    return {
      path: cleanedPath,
      flowsIds: cleanedFlows,
    };
  }

  private updateUrl({ path, flowsIds }: UrlParams): void {
    // TODO: Improve this with Angular Router
    // Hack to add the tab as Fragment part of the URL
    const flowsQueryParams = (flowsIds ?? []).map((value) => `flows=${value}`).join('&');

    const queryParams = flowsQueryParams.length > 0 ? `?${flowsQueryParams}` : '';

    this.location.go(`${path}${queryParams}`);
  }

  get isLoading() {
    return this.apiDefinition == null;
  }
}
