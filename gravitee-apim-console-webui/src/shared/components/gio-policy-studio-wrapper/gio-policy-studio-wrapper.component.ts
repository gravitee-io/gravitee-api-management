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
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Location } from '@angular/common';
import { catchError, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { chain, isNil } from 'lodash';

import { FlowService } from '../../../services-ngx/flow.service';
import { FlowConfigurationSchema } from '../../../entities/flow/configurationSchema';
import { PolicyService } from '../../../services-ngx/policy.service';
import { ResourceService } from '../../../services-ngx/resource.service';
import { SpelService } from '../../../services-ngx/spel.service';
import { Grammar } from '../../../entities/spel/grammar';
import { PolicyListItem } from '../../../entities/policy';

import '@gravitee/ui-components/wc/gv-policy-studio';

interface UrlParams {
  path: string;
  tabId: string;
  flowsIds: string[];
}

@Component({
  selector: 'gio-policy-studio-wrapper',
  template: require('./gio-policy-studio-wrapper.component.html'),
  styles: [require('./gio-policy-studio-wrapper.component.scss')],
})
export class GioPolicyStudioWrapperComponent implements OnInit {
  @Input()
  canAdd: boolean;

  @Input()
  canDebug: boolean;

  @Input()
  hasResources: boolean;

  @Input()
  hasProperties: boolean;

  @Input()
  hasPolicyFilter: boolean;

  @Input()
  sortable: boolean;

  @Input()
  set policies(policies: PolicyListItem[]) {
    this._policies = chain(policies)
      .map((policy) => (!isNil(policy.category) ? policy : { ...policy, category: this.unknownPolicyCategory }))
      // First sort by category (based on category order) and then by name
      .sortBy([(policy) => this.policyCategoriesOrder.indexOf(policy.category), (policy) => policy.name])
      .value();
  }

  get policies() {
    return this._policies;
  }

  @Input()
  definition: unknown;

  @Input()
  services: Record<string, unknown> = {};

  @Input()
  flowSchema: unknown;

  @Input()
  resourceTypes: unknown[];

  @Input()
  propertyProviders: unknown[];

  @Input()
  readonlyPlans: boolean;

  @Input()
  dynamicPropertySchema: unknown = {};

  @Input()
  debugResponse: unknown;

  @Input()
  flowsTitle: string;

  @Input()
  hasConditionalSteps: boolean;

  @Input()
  configurationInformation =
    'By default, the selection of a flow is based on the operator defined in the flow itself. This operator allows either to select a flow when the path matches exactly, or when the start of the path matches. The "Best match" option allows you to select the flow from the path that is closest.';

  @Output()
  save = new EventEmitter<unknown>();

  tabId: string;
  selectedFlowsIds: string;
  configurationSchema: FlowConfigurationSchema;
  policyDocumentation: { id: string; image: string; content: string };

  private readonly unknownPolicyCategory = 'others';
  private readonly policyCategoriesOrder = ['security', 'performance', 'transformation', this.unknownPolicyCategory];
  private _policies: PolicyListItem[];

  constructor(
    private readonly location: Location,
    private readonly flowService: FlowService,
    private readonly policyService: PolicyService,
    private readonly resourceService: ResourceService,
    private readonly spelService: SpelService,
  ) {}

  ngOnInit(): void {
    this.flowService
      .getConfigurationSchemaForm()
      .pipe(
        tap((configurationSchema) => {
          this.configurationSchema = configurationSchema;
        }),
      )
      .subscribe();

    const { tabId, flowsIds } = this.parseUrl();

    this.tabId = tabId;
    this.selectedFlowsIds = JSON.stringify(flowsIds);
  }

  public onTabChanged(tabId: string): void {
    this.updateUrl({ ...this.parseUrl(), tabId });
  }

  public onFlowSelectionChanged({ flows }: { flows: string[] }): void {
    this.updateUrl({ ...this.parseUrl(), flowsIds: flows });
  }

  public fetchPolicyDocumentation({ policy }: { policy: { id: string; icon: string } }): void {
    this.policyService
      .getDocumentation(policy.id)
      .pipe(tap((documentation) => (this.policyDocumentation = { id: policy.id, image: policy.icon, content: documentation })))
      .subscribe();
  }

  public fetchResourceDocumentation({
    resourceType,
    target,
  }: {
    resourceType: { id: string; icon: string };
    target: { documentation: any };
  }): void {
    this.resourceService
      .getDocumentation(resourceType.id)
      .pipe(
        tap((documentation) => (target.documentation = { image: resourceType.icon, content: documentation })),
        catchError(() => {
          target.documentation = null;
          return EMPTY;
        }),
      )
      .subscribe();
  }

  public fetchSpelGrammar({ currentTarget }: { currentTarget: { grammar: Grammar } }): void {
    this.spelService
      .getGrammar()
      .pipe(tap((grammar) => (currentTarget.grammar = grammar)))
      .subscribe();
  }

  private parseUrl(): UrlParams {
    // TODO: Improve this with Angular Router
    // Hack to add the tab as Fragment part of the URL
    const [path, tabId] = this.location.path(true).split(/#(\w*)$/);

    const [basePath, ...flowsIds] = path.split('flows');

    const cleanedPath = basePath.replace('?', '');
    const cleanedFlows = (flowsIds ?? []).map((flow) => flow.replace('=', ''));

    return {
      path: cleanedPath,
      tabId: tabId ?? '',
      flowsIds: cleanedFlows,
    };
  }

  private updateUrl({ path, tabId, flowsIds }: UrlParams): void {
    // TODO: Improve this with Angular Router
    // Hack to add the tab as Fragment part of the URL
    const flowsQueryParams = (flowsIds ?? []).map((value) => `flows=${value}`).join('&');

    const queryParams = flowsQueryParams.length > 0 ? `?${flowsQueryParams}` : '';

    this.location.go(`${path}${queryParams}#${tabId}`);
  }
}
