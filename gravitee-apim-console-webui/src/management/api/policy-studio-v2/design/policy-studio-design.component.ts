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
import '@gravitee/ui-components/wc/gv-design';
import { MatDialog } from '@angular/material/dialog';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';
import { castArray } from 'lodash';

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
import { ApimFeature, UTMTags } from '../../../../shared/components/gio-license/gio-license-data';

export interface UrlParams {
  path: string;
  flowsIds: string[];
}

@Component({
  selector: 'policy-studio-design',
  templateUrl: './policy-studio-design.component.html',
  styleUrls: ['./policy-studio-design.component.scss'],
  standalone: false,
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
  policyDocumentation!: { id: string; image: string; content: string; type: string };
  selectedFlowsIds: Array<string> = [];

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
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

    const flowsIds = this.activatedRoute.snapshot.queryParams?.flows;
    if (flowsIds) {
      this.selectedFlowsIds = castArray(flowsIds);
    }
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
    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: { flows },
    });
  }

  public displayPolicyCta() {
    this.licenseService.openDialog({ feature: ApimFeature.APIM_POLICY_V2, context: UTMTags.CONTEXT_API });
  }

  public fetchPolicyDocumentation({ policy }: { policy: { id: string; icon: string } }): void {
    this.policyStudioDesignService
      .getDocumentation(policy.id)
      .pipe(
        tap(
          documentation =>
            (this.policyDocumentation = {
              id: policy.id,
              image: policy.icon,
              content: documentation.content,
              type: documentation.language === 'ASCIIDOC' ? 'adoc' : 'md',
            }),
        ),
      )
      .subscribe();
  }

  public fetchSpelGrammar({ currentTarget }: { currentTarget: { grammar: Grammar } }): void {
    this.policyStudioDesignService
      .getSpelGrammar()
      .pipe(tap(grammar => (currentTarget.grammar = grammar)))
      .subscribe();
  }

  get isLoading() {
    return this.apiDefinition == null;
  }
}
