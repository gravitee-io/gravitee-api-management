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

import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { ComponentCustomEvent } from '@gravitee/ui-components/src/lib/events';
import { Location } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import '@gravitee/ui-components/wc/gv-design';

import { UrlParams } from '../../../../management/api/policy-studio-v2/design/policy-studio-design.component';
import { OrganizationService } from '../../../../services-ngx/organization.service';
import { OrgSettingsPlatformPoliciesService } from '../org-settings-platform-policies.service';
import { Organization } from '../../../../entities/organization/organization';
import { DefinitionVM } from '../org-settings-platform-policies.component';
import { PlatformFlowSchema } from '../../../../entities/flow/platformFlowSchema';
import { PolicyListItem } from '../../../../entities/policy';
import { ResourceListItem } from '../../../../entities/resource/resourceListItem';
import { Grammar } from '../../../../entities/spel/grammar';
import { ApimFeature, UTMTags } from '../../../../shared/components/gio-license/gio-license-data';

@Component({
  selector: 'org-settings-platform-policies-studio',
  templateUrl: './org-settings-platform-policies-studio.component.html',
  styleUrls: ['./org-settings-platform-policies-studio.component.scss'],
  standalone: false,
})
export class OrgSettingsPlatformPoliciesStudioComponent implements OnInit, OnDestroy {
  get isLoading() {
    return !this.definition;
  }

  organization: Organization;
  definition: DefinitionVM;
  platformFlowSchema: PlatformFlowSchema;
  policies: PolicyListItem[];
  resourceTypes: ResourceListItem[];
  selectedFlowsIds: string[] = [];
  policyDocumentation!: { id: string; image: string; content: string };

  @Output()
  change = new EventEmitter<DefinitionVM['flows']>();

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly location: Location,
    private readonly organizationService: OrganizationService,
    private readonly orgSettingsPlatformPoliciesService: OrgSettingsPlatformPoliciesService,
    private readonly licenseService: GioLicenseService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.orgSettingsPlatformPoliciesService.getPlatformFlowSchemaForm(),
      this.orgSettingsPlatformPoliciesService.listPolicies({ expandSchema: true, expandIcon: true }),
      this.organizationService.get(),
      this.orgSettingsPlatformPoliciesService.listResources({ expandSchema: true, expandIcon: true }),
    ])
      .pipe(
        tap(([flowSchema, policies, organization, resourceTypes]) => {
          this.platformFlowSchema = flowSchema;
          this.policies = policies;
          this.organization = organization;

          this.definition = {
            flows: (this.organization.flows ?? []).map((flow) => ({
              ...flow,
              consumers: flow.consumers.map((consumer) => consumer.consumerId),
            })),
          };

          this.resourceTypes = resourceTypes;
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

  onFlowSelectionChanged({ flows }: { flows: string[] }): void {
    this.updateUrl({ ...this.parseUrl(), flowsIds: flows });
  }

  fetchPolicyDocumentation({ policy }: { policy: { id: string; icon: string } }): void {
    this.orgSettingsPlatformPoliciesService
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

  displayPolicyCTA() {
    const licenseOptions = { feature: ApimFeature.APIM_POLICY_V2, context: UTMTags.CONTEXT_ORGANIZATION };
    this.licenseService.openDialog(licenseOptions);
  }

  fetchSpelGrammar({ currentTarget }: { currentTarget: { grammar: Grammar } }): void {
    this.orgSettingsPlatformPoliciesService
      .getSpelGrammar()
      .pipe(tap((grammar) => (currentTarget.grammar = grammar)))
      .subscribe();
  }

  onChange(
    $event: ComponentCustomEvent<{
      isDirty: boolean;
      errors: number;
      definition?: DefinitionVM;
    }>,
  ) {
    const { isDirty, errors, definition } = $event.detail;
    if (isDirty && errors === 0 && definition != null) {
      this.change.emit(definition.flows);
    }
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
}
