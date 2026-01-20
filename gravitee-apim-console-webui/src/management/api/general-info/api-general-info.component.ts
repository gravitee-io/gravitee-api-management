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
import { Component, Inject, OnDestroy, OnInit, AfterViewInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, EMPTY, of, Subject, throwError, merge } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GIO_DIALOG_WIDTH, NewFile } from '@gravitee/ui-particles-angular';

import {
  ApiGeneralInfoDuplicateDialogComponent,
  ApiPortalDetailsDuplicateDialogData,
} from './api-general-info-duplicate-dialog/api-general-info-duplicate-dialog.component';
import {
  ApiGeneralInfoExportV2DialogComponent,
  ApiPortalDetailsExportV2DialogData,
} from './api-general-info-export-v2-dialog/api-general-info-export-v2-dialog.component';
import {
  ApiGeneralInfoPromoteDialogComponent,
  ApiPortalDetailsPromoteDialogData,
} from './api-general-info-promote-dialog/api-general-info-promote-dialog.component';
import {
  ApiGeneralDetailsExportV4DialogData,
  ApiGeneralDetailsExportV4DialogResult,
  ApiGeneralInfoExportV4DialogComponent,
} from './api-general-info-export-v4-dialog/api-general-info-export-v4-dialog.component';
import { ApiGeneralInfoMigrateToV4DialogComponent } from './api-general-info-migrate-to-v4-dialog/api-general-info-migrate-to-v4-dialog.component';

import { Category } from '../../../entities/category/Category';
import { Constants } from '../../../entities/Constants';
import { CategoryService } from '../../../services-ngx/category.service';
import { PolicyService } from '../../../services-ngx/policy.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioApiImportDialogComponent, GioApiImportDialogData } from '../component/gio-api-import-dialog/gio-api-import-dialog.component';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api, ApiType, ApiV2, ApiV4, UpdateApi, UpdateApiV2, UpdateApiV4, Plan } from '../../../entities/management-api-v2';
import { MigrateToV4State } from '../../../entities/management-api-v2/api/v2/migrateToV4Response';
import { HttpListener } from '../../../entities/management-api-v2/api/v4/httpListener';
import { Integration } from '../../integrations/integrations.model';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { ApiPlanV2Service } from '../../../services-ngx/api-plan-v2.service';

export interface MigrateDialogResult {
  confirmed: boolean;
  state: MigrateToV4State;
}

@Component({
  selector: 'api-general-info',
  templateUrl: './api-general-info.component.html',
  styleUrls: ['./api-general-info.component.scss'],
  standalone: false,
})
export class ApiGeneralInfoComponent implements OnInit, OnDestroy, AfterViewInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @ViewChild('mermaidDiagram', { static: false }) mermaidDiagram: ElementRef;

  public apiId: string;
  public api: Api;
  public apiType: ApiType;
  public mermaidDiagramDefinition: string = '';
  public isMermaidLoaded: boolean = false;
  public plans: Plan[] = [];
  private nodeClickHandlers: Map<string, () => void> = new Map();

  public apiDetailsForm: UntypedFormGroup;
  public apiImagesForm: UntypedFormGroup;
  public parentForm: UntypedFormGroup;
  public initialApiDetailsFormValue: unknown;
  public labelsAutocompleteOptions: string[] = [];
  public apiCategories: Category[] = [];
  public apiOwner: string;
  public apiCreatedAt: Date;
  public apiLastDeploymentAt: Date;
  public dangerActions = {
    canAskForReview: false,
    canStartApi: false,
    canStopApi: false,
    canChangeApiLifecycle: false,
    canPublish: false,
    canUnpublish: false,
    canChangeVisibilityToPublic: false,
    canChangeVisibilityToPrivate: false,
    canDeprecate: false,
    canDelete: false,
  };
  public cannotPromote = true;
  public canDisplayV4EmulationEngineToggle = false;

  public isQualityEnabled = false;
  public isQualitySupported = false;

  public isReadOnly = false;
  public isKubernetesOrigin = false;

  public integrationName = '';
  public integrationId = '';

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly policyService: PolicyService,
    private readonly categoryService: CategoryService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly integrationsService: IntegrationsService,
    private readonly planService: ApiPlanV2Service,
    private readonly cdr: ChangeDetectorRef,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  private refresh$ = new Subject<void>();

  ngOnInit(): void {
    this.labelsAutocompleteOptions = this.constants.env?.settings?.api?.labelsDictionary ?? [];

    this.isQualityEnabled = this.constants.env?.settings?.apiQualityMetrics?.enabled;

    merge(this.activatedRoute.params, this.refresh$.pipe(map(() => this.activatedRoute.snapshot.params)))
      .pipe(
        switchMap((params) => {
          this.apiId = params.apiId;
          return combineLatest([this.apiService.get(this.apiId), this.categoryService.list()]);
        }),
        switchMap(([api, categories]) =>
          combineLatest([isImgUrl(api._links['pictureUrl']), isImgUrl(api._links['backgroundUrl'])]).pipe(
            map(
              ([hasPictureImg, hasBackgroundImg]) =>
                // FIXME:create type ApiVM?
                [
                  {
                    ...api,
                    _links: {
                      ...api._links,
                      pictureUrl: hasPictureImg ? api._links['pictureUrl'] : null,
                      backgroundUrl: hasBackgroundImg ? api._links['backgroundUrl'] : null,
                    },
                  },
                  categories,
                ] as const,
            ),
          ),
        ),
        tap(([api, categories]) => {
          this.isKubernetesOrigin = api.originContext?.origin === 'KUBERNETES';

          if (api.definitionVersion === 'V4') {
            this.apiType = (api as ApiV4).type;
          }

          this.isReadOnly =
            !this.permissionService.hasAnyMatching(['api-definition-u']) || this.isKubernetesOrigin || api.definitionVersion === 'V1';

          this.api = api;

          this.apiCategories = categories;
          this.apiOwner = api.primaryOwner.displayName;
          this.apiCreatedAt = api.createdAt;
          this.apiLastDeploymentAt = api.updatedAt;

          this.dangerActions = {
            canAskForReview:
              this.constants.env?.settings?.apiReview?.enabled &&
              (api.workflowState === 'DRAFT' || api.workflowState === 'REQUEST_FOR_CHANGES' || !api.workflowState),
            canStartApi:
              (!this.constants.env?.settings?.apiReview?.enabled ||
                (this.constants.env?.settings?.apiReview?.enabled && (!api.workflowState || api.workflowState === 'REVIEW_OK'))) &&
              api.state === 'STOPPED',
            canStopApi:
              (!this.constants.env?.settings?.apiReview?.enabled ||
                (this.constants.env?.settings?.apiReview?.enabled && (!api.workflowState || api.workflowState === 'REVIEW_OK'))) &&
              api.state === 'STARTED',

            canChangeApiLifecycle: this.canChangeApiLifecycle(api),
            canPublish: !api.lifecycleState || api.lifecycleState === 'CREATED' || api.lifecycleState === 'UNPUBLISHED',
            canUnpublish: api.lifecycleState === 'PUBLISHED',

            canChangeVisibilityToPublic: api.lifecycleState !== 'DEPRECATED' && api.visibility === 'PRIVATE',
            canChangeVisibilityToPrivate: api.lifecycleState !== 'DEPRECATED' && api.visibility === 'PUBLIC',
            canDeprecate: api.lifecycleState !== 'DEPRECATED',
            canDelete: !(api.state === 'STARTED' || api.lifecycleState === 'PUBLISHED'),
          };
          this.canDisplayV4EmulationEngineToggle = (api.definitionVersion != null && api.definitionVersion === 'V2') ?? false;
          this.cannotPromote =
            !(this.dangerActions.canChangeApiLifecycle && api.lifecycleState !== 'DEPRECATED') ||
            this.isKubernetesOrigin ||
            this.api.definitionVersion === 'V1';

          this.apiDetailsForm = new UntypedFormGroup({
            name: new UntypedFormControl(
              {
                value: api.name,
                disabled: this.isReadOnly,
              },
              [Validators.required],
            ),
            version: new UntypedFormControl(
              {
                value: api.apiVersion,
                disabled: this.isReadOnly,
              },
              [Validators.required],
            ),
            description: new UntypedFormControl({
              value: api.description,
              disabled: this.isReadOnly,
            }),
            labels: new UntypedFormControl({
              value: api.labels,
              disabled: this.isReadOnly,
            }),
            categories: new UntypedFormControl({
              value: api.categories,
              disabled: this.isReadOnly,
            }),
            emulateV4Engine: new UntypedFormControl({
              value: api.definitionVersion === 'V2' && (api as ApiV2).executionMode === 'V4_EMULATION_ENGINE',
              disabled: this.isReadOnly,
            }),
          });
          this.apiImagesForm = new UntypedFormGroup({
            picture: new UntypedFormControl({
              value: api._links['pictureUrl'] ? [api._links['pictureUrl']] : [],
              disabled: this.isReadOnly,
            }),
            background: new UntypedFormControl({
              value: api._links['backgroundUrl'] ? [api._links['backgroundUrl']] : [],
              disabled: this.isReadOnly,
            }),
          });
          this.parentForm = new UntypedFormGroup({
            details: this.apiDetailsForm,
            images: this.apiImagesForm,
          });

          this.initialApiDetailsFormValue = this.parentForm.getRawValue();
          this.isQualitySupported = this.api.definitionVersion === 'V2' || this.api.definitionVersion === 'V1';

          // Generate diagram when API is loaded
          if (api && api.definitionVersion === 'V4') {
            // Fetch plans for the API
            this.planService
              .list(api.id, undefined, undefined, undefined, 1, 100)
              .pipe(
                takeUntil(this.unsubscribe$),
                tap((plansResponse) => {
                  this.plans = plansResponse.data || [];
                  // Regenerate diagram with plans
                  this.generateMermaidDiagram(api as ApiV4);
                }),
                catchError((error) => {
                  console.error('Error fetching plans:', error);
                  // Still generate diagram without plans
                  this.plans = [];
                  this.generateMermaidDiagram(api as ApiV4);
                  return of(null);
                }),
              )
              .subscribe();
          }
        }),
        switchMap(([api]) => {
          if ('integrationId' in api.originContext) {
            return this.integrationsService.getIntegration(api.originContext.integrationId);
          }
          return of(null);
        }),
        tap((integration: Integration | null) => {
          if (integration) {
            this.integrationName = integration.name;
            this.integrationId = integration.id;
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  ngAfterViewInit(): void {
    this.loadMermaidAndRender();
  }

  private async loadMermaidAndRender(): Promise<void> {
    try {
      // Check if mermaid is already loaded
      if ((window as any).mermaid) {
        this.isMermaidLoaded = true;
        this.renderDiagram();
        return;
      }

      // Try to load mermaid from node_modules
      try {
        const mermaidModule = await import('mermaid');
        // Handle different mermaid module structures
        let mermaid: any;
        if (mermaidModule.default) {
          mermaid = mermaidModule.default;
        } else if (typeof mermaidModule === 'function') {
          mermaid = mermaidModule;
        } else if ((mermaidModule as any).mermaid) {
          mermaid = (mermaidModule as any).mermaid;
        } else {
          mermaid = mermaidModule;
        }

        if (mermaid && typeof mermaid.initialize === 'function') {
          (window as any).mermaid = mermaid;
          mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose' });
          this.isMermaidLoaded = true;
          this.renderDiagram();
          return;
        } else if (mermaid && typeof (mermaid as any).init === 'function') {
          // Some versions use 'init' instead of 'initialize'
          (window as any).mermaid = mermaid;
          (mermaid as any).init({ startOnLoad: false, theme: 'default', securityLevel: 'loose' });
          this.isMermaidLoaded = true;
          this.renderDiagram();
          return;
        }
      } catch (e) {
        console.log('Mermaid not found in node_modules, loading from CDN', e);
      }

      // Fallback to CDN
      if (!(window as any).mermaid) {
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js';
        script.onload = () => {
          const mermaid = (window as any).mermaid;
          if (mermaid) {
            if (typeof mermaid.initialize === 'function') {
              mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose' });
            } else if (typeof mermaid.init === 'function') {
              mermaid.init({ startOnLoad: false, theme: 'default', securityLevel: 'loose' });
            }
            this.isMermaidLoaded = true;
            this.renderDiagram();
          }
        };
        script.onerror = () => {
          console.error('Failed to load Mermaid from CDN');
        };
        document.head.appendChild(script);
      }
    } catch (error) {
      console.error('Error loading Mermaid:', error);
    }
  }

  private renderDiagram(): void {
    if (!this.isMermaidLoaded || !this.mermaidDiagramDefinition || !this.mermaidDiagram) {
      return;
    }

    setTimeout(() => {
      const element = this.mermaidDiagram.nativeElement;
      if (!element || !this.mermaidDiagramDefinition) {
        return;
      }

      const mermaid = (window as any).mermaid;
      if (!mermaid) {
        return;
      }

      try {
        element.innerHTML = '';
        const id = 'mermaid-' + Date.now();

        // Use mermaid.render for async rendering
        mermaid
          .render(id, this.mermaidDiagramDefinition)
          .then((result: { svg: string }) => {
            element.innerHTML = result.svg;

            // Add click handlers to nodes after rendering
            setTimeout(() => {
              this.addClickHandlersToDiagram(element);
            }, 100);
          })
          .catch((error: any) => {
            console.error('Error rendering Mermaid diagram:', error);
            element.innerHTML = '<p>Error rendering diagram. Please check the console for details.</p>';
          });
      } catch (error) {
        console.error('Error in renderDiagram:', error);
        element.innerHTML = '<p>Error rendering diagram. Please check the console for details.</p>';
      }
    }, 200);
  }

  private generateMermaidDiagram(api: ApiV4): void {
    if (!api) {
      return;
    }

    // Clear previous click handlers
    this.nodeClickHandlers.clear();

    let diagram = 'graph TB\n';
    diagram += `    Start([API: ${api.name || 'Unnamed'}<br/>Version: ${api.definitionVersion || 'V4'}<br/>Type: ${api.type || 'PROXY'}]) --> Listeners\n`;

    // Add Listeners
    if (api.listeners && api.listeners.length > 0) {
      api.listeners.forEach((listener, idx) => {
        const listenerId = `Listener${idx}`;
        diagram += `    Listeners --> ${listenerId}[Listener ${idx + 1}<br/>Type: ${listener.type || 'HTTP'}]\n`;

        // Add Paths (only for HTTP listeners)
        if (listener.type === 'HTTP') {
          const httpListener = listener as HttpListener;
          if (httpListener.paths && httpListener.paths.length > 0) {
            httpListener.paths.forEach((path, pathIdx) => {
              const pathId = `Path${idx}_${pathIdx}`;
              diagram += `    ${listenerId} --> ${pathId}["Path: ${path.path || '/'}"]\n`;
            });
          }
        }

        // Add Entrypoints
        if (listener.entrypoints && listener.entrypoints.length > 0) {
          listener.entrypoints.forEach((entrypoint, epIdx) => {
            const epId = `Entrypoint${idx}_${epIdx}`;
            diagram += `    ${listenerId} --> ${epId}["Entrypoint: ${entrypoint.type || 'http-proxy'}<br/>QoS: ${entrypoint.qos || 'AUTO'}"]\n`;
          });
        }
      });
    }

    // Add Endpoint Groups
    if (api.endpointGroups && api.endpointGroups.length > 0) {
      diagram += `    Listeners --> EndpointGroups\n`;
      api.endpointGroups.forEach((group, groupIdx) => {
        const groupId = `EndpointGroup${groupIdx}`;
        diagram += `    EndpointGroups --> ${groupId}["Endpoint Group: ${group.name || 'Default'}<br/>Type: ${group.type || 'http-proxy'}<br/>Load Balancer: ${group.loadBalancer?.type || 'ROUND_ROBIN'}"]\n`;

        // Add Endpoints
        if (group.endpoints && group.endpoints.length > 0) {
          group.endpoints.forEach((endpoint, epIdx) => {
            const epId = `Endpoint${groupIdx}_${epIdx}`;
            const target = (endpoint.configuration as any)?.target || 'N/A';
            diagram += `    ${groupId} --> ${epId}["Endpoint: ${endpoint.name || 'Default'}<br/>Target: ${target}<br/>Weight: ${endpoint.weight || 1}"]\n`;
          });
        }
      });
    }

    // Add Flow Execution
    if (api.flowExecution) {
      diagram += `    Listeners --> FlowExecution["Flow Execution<br/>Mode: ${api.flowExecution.mode || 'DEFAULT'}<br/>Match Required: ${api.flowExecution.matchRequired || false}"]\n`;
    }

    // Add Flows (Policies)
    if (api.flows && api.flows.length > 0) {
      diagram += `    FlowExecution --> Flows\n`;
      api.flows.forEach((flow, flowIdx) => {
        const flowId = `Flow${flowIdx}`;
        const flowName = flow.name || `Flow ${flowIdx + 1}`;
        const enabledStatus = flow.enabled !== false ? 'Enabled' : 'Disabled';
        diagram += `    Flows --> ${flowId}["${flowName}<br/>Status: ${enabledStatus}"]\n`;

        // Add click handler for API-level flows (planIndex=0 for API flows)
        this.nodeClickHandlers.set(flowId, () => {
          this.router.navigate(['v4/policy-studio', '0', flowIdx.toString()], { relativeTo: this.activatedRoute });
        });

        // Add Selectors
        if (flow.selectors && flow.selectors.length > 0) {
          flow.selectors.forEach((selector, selIdx) => {
            const selId = `Selector${flowIdx}_${selIdx}`;
            if (selector.type === 'HTTP' && 'path' in selector) {
              const httpSelector = selector as any;
              const path = httpSelector.path || '/';
              const pathOp = httpSelector.pathOperator || 'EQUALS';
              const methods = httpSelector.methods && httpSelector.methods.length > 0 ? httpSelector.methods.join(', ') : 'ALL';
              diagram += `    ${flowId} --> ${selId}["Selector: ${path}<br/>Operator: ${pathOp}<br/>Methods: ${methods}"]\n`;
            } else {
              diagram += `    ${flowId} --> ${selId}["Selector: ${selector.type || 'UNKNOWN'}"]\n`;
            }
          });
        }

        // Add Request Policies
        if (flow.request && flow.request.length > 0) {
          const requestId = `Request${flowIdx}`;
          diagram += `    ${flowId} --> ${requestId}["Request Policies"]\n`;
          flow.request.forEach((step, stepIdx) => {
            const stepId = `RequestStep${flowIdx}_${stepIdx}`;
            const stepName = step.name || step.policy || 'Policy';
            const stepEnabled = step.enabled !== false ? '✓' : '✗';
            diagram += `    ${requestId} --> ${stepId}["${stepName} ${stepEnabled}<br/>Policy: ${step.policy || 'N/A'}"]\n`;
          });
        }

        // Add Response Policies
        if (flow.response && flow.response.length > 0) {
          const responseId = `Response${flowIdx}`;
          diagram += `    ${flowId} --> ${responseId}["Response Policies"]\n`;
          flow.response.forEach((step, stepIdx) => {
            const stepId = `ResponseStep${flowIdx}_${stepIdx}`;
            const stepName = step.name || step.policy || 'Policy';
            const stepEnabled = step.enabled !== false ? '✓' : '✗';
            diagram += `    ${responseId} --> ${stepId}["${stepName} ${stepEnabled}<br/>Policy: ${step.policy || 'N/A'}"]\n`;
          });
        }

        // Add Subscribe Policies
        if (flow.subscribe && flow.subscribe.length > 0) {
          const subscribeId = `Subscribe${flowIdx}`;
          diagram += `    ${flowId} --> ${subscribeId}["Subscribe Policies"]\n`;
          flow.subscribe.forEach((step, stepIdx) => {
            const stepId = `SubscribeStep${flowIdx}_${stepIdx}`;
            const stepName = step.name || step.policy || 'Policy';
            const stepEnabled = step.enabled !== false ? '✓' : '✗';
            diagram += `    ${subscribeId} --> ${stepId}["${stepName} ${stepEnabled}<br/>Policy: ${step.policy || 'N/A'}"]\n`;
          });
        }

        // Add Publish Policies
        if (flow.publish && flow.publish.length > 0) {
          const publishId = `Publish${flowIdx}`;
          diagram += `    ${flowId} --> ${publishId}["Publish Policies"]\n`;
          flow.publish.forEach((step, stepIdx) => {
            const stepId = `PublishStep${flowIdx}_${stepIdx}`;
            const stepName = step.name || step.policy || 'Policy';
            const stepEnabled = step.enabled !== false ? '✓' : '✗';
            diagram += `    ${publishId} --> ${stepId}["${stepName} ${stepEnabled}<br/>Policy: ${step.policy || 'N/A'}"]\n`;
          });
        }
      });
    }

    // Add Plan Flows
    if (this.plans && this.plans.length > 0) {
      diagram += `    Start --> Plans["Plans"]\n`;
      this.plans.forEach((plan, planIdx) => {
        const planId = `Plan${planIdx}`;
        const planName = plan.name || `Plan ${planIdx + 1}`;
        const planStatus = plan.status || 'UNKNOWN';
        const planMode = (plan as any).mode || 'STANDARD';
        diagram += `    Plans --> ${planId}["${planName}<br/>Status: ${planStatus}<br/>Mode: ${planMode}"]\n`;

        // Add click handler for plan - navigate to plan edit page
        if (plan.id) {
          this.nodeClickHandlers.set(planId, () => {
            this.router.navigate(['plans', plan.id], { relativeTo: this.activatedRoute });
          });
        }

        // Add flows from this plan (only V4 plans have flows in the same format)
        if (plan.definitionVersion === 'V4') {
          const planV4 = plan as any; // PlanV4
          if (planV4.flows && planV4.flows.length > 0) {
            const planFlowsId = `PlanFlows${planIdx}`;
            diagram += `    ${planId} --> ${planFlowsId}["Plan Flows"]\n`;

            planV4.flows.forEach((flow: any, flowIdx: number) => {
              const flowId = `PlanFlow${planIdx}_${flowIdx}`;
              const flowName = flow.name || `Flow ${flowIdx + 1}`;
              const enabledStatus = flow.enabled !== false ? 'Enabled' : 'Disabled';
              diagram += `    ${planFlowsId} --> ${flowId}["${flowName}<br/>Status: ${enabledStatus}"]\n`;

              // Add click handler for plan-level flows (planIndex=planIdx+1, since 0 is for API flows)
              this.nodeClickHandlers.set(flowId, () => {
                this.router.navigate(['v4/policy-studio', (planIdx + 1).toString(), flowIdx.toString()], {
                  relativeTo: this.activatedRoute,
                });
              });

              // Add Selectors
              if (flow.selectors && flow.selectors.length > 0) {
                flow.selectors.forEach((selector, selIdx) => {
                  const selId = `PlanSelector${planIdx}_${flowIdx}_${selIdx}`;
                  if (selector.type === 'HTTP' && 'path' in selector) {
                    const httpSelector = selector as any;
                    const path = httpSelector.path || '/';
                    const pathOp = httpSelector.pathOperator || 'EQUALS';
                    const methods = httpSelector.methods && httpSelector.methods.length > 0 ? httpSelector.methods.join(', ') : 'ALL';
                    diagram += `    ${flowId} --> ${selId}["Selector: ${path}<br/>Operator: ${pathOp}<br/>Methods: ${methods}"]\n`;
                  } else {
                    diagram += `    ${flowId} --> ${selId}["Selector: ${selector.type || 'UNKNOWN'}"]\n`;
                  }
                });
              }

              // Add Request Policies
              if (flow.request && flow.request.length > 0) {
                const requestId = `PlanRequest${planIdx}_${flowIdx}`;
                diagram += `    ${flowId} --> ${requestId}["Request Policies"]\n`;
                flow.request.forEach((step, stepIdx) => {
                  const stepId = `PlanRequestStep${planIdx}_${flowIdx}_${stepIdx}`;
                  const stepName = step.name || step.policy || 'Policy';
                  const stepEnabled = step.enabled !== false ? '✓' : '✗';
                  diagram += `    ${requestId} --> ${stepId}["${stepName} ${stepEnabled}<br/>Policy: ${step.policy || 'N/A'}"]\n`;
                });
              }

              // Add Response Policies
              if (flow.response && flow.response.length > 0) {
                const responseId = `PlanResponse${planIdx}_${flowIdx}`;
                diagram += `    ${flowId} --> ${responseId}["Response Policies"]\n`;
                flow.response.forEach((step, stepIdx) => {
                  const stepId = `PlanResponseStep${planIdx}_${flowIdx}_${stepIdx}`;
                  const stepName = step.name || step.policy || 'Policy';
                  const stepEnabled = step.enabled !== false ? '✓' : '✗';
                  diagram += `    ${responseId} --> ${stepId}["${stepName} ${stepEnabled}<br/>Policy: ${step.policy || 'N/A'}"]\n`;
                });
              }

              // Add Subscribe Policies
              if (flow.subscribe && flow.subscribe.length > 0) {
                const subscribeId = `PlanSubscribe${planIdx}_${flowIdx}`;
                diagram += `    ${flowId} --> ${subscribeId}["Subscribe Policies"]\n`;
                flow.subscribe.forEach((step, stepIdx) => {
                  const stepId = `PlanSubscribeStep${planIdx}_${flowIdx}_${stepIdx}`;
                  const stepName = step.name || step.policy || 'Policy';
                  const stepEnabled = step.enabled !== false ? '✓' : '✗';
                  diagram += `    ${subscribeId} --> ${stepId}["${stepName} ${stepEnabled}<br/>Policy: ${step.policy || 'N/A'}"]\n`;
                });
              }

              // Add Publish Policies
              if (flow.publish && flow.publish.length > 0) {
                const publishId = `PlanPublish${planIdx}_${flowIdx}`;
                diagram += `    ${flowId} --> ${publishId}["Publish Policies"]\n`;
                flow.publish.forEach((step, stepIdx) => {
                  const stepId = `PlanPublishStep${planIdx}_${flowIdx}_${stepIdx}`;
                  const stepName = step.name || step.policy || 'Policy';
                  const stepEnabled = step.enabled !== false ? '✓' : '✗';
                  diagram += `    ${publishId} --> ${stepId}["${stepName} ${stepEnabled}<br/>Policy: ${step.policy || 'N/A'}"]\n`;
                });
              }
            });
          }
        }
      });
    }

    // Add Analytics
    if (api.analytics) {
      diagram += `    Start --> Analytics["Analytics<br/>Enabled: ${api.analytics.enabled || false}"]\n`;
    }

    this.mermaidDiagramDefinition = diagram;
    this.cdr.detectChanges();

    // Render after a short delay to ensure view is ready
    setTimeout(() => {
      this.renderDiagram();
    }, 200);
  }

  private addClickHandlersToDiagram(element: HTMLElement): void {
    const svg = element.querySelector('svg');
    if (!svg) {
      return;
    }

    // Find all node groups in the SVG - Mermaid wraps nodes in g.node
    const nodeGroups = svg.querySelectorAll('g.node');

    // Create a mapping of expected node patterns to handlers
    // We'll match by node ID pattern since Mermaid uses consistent naming
    this.nodeClickHandlers.forEach((handler, nodeId) => {
      // Mermaid generates node IDs with lowercase and specific patterns
      // Try multiple selectors to catch different ID formats
      const patterns = [`[id*="${nodeId.toLowerCase()}"]`, `[id*="${nodeId}"]`, `#${nodeId.toLowerCase()}`, `#${nodeId}`];

      patterns.forEach((pattern) => {
        try {
          const matches = svg.querySelectorAll(pattern);
          matches.forEach((match) => {
            // Find the parent g.node element
            let nodeGroup = match;
            while (nodeGroup && nodeGroup.nodeName !== 'g') {
              nodeGroup = nodeGroup.parentElement;
            }

            if (nodeGroup && nodeGroup.classList.contains('node') && !nodeGroup.getAttribute('data-clickable')) {
              const nodeEl = nodeGroup as HTMLElement;
              nodeEl.style.cursor = 'pointer';

              // Add click handler
              nodeGroup.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                handler();
              });

              // Add hover effects
              nodeGroup.addEventListener('mouseenter', () => {
                nodeEl.style.opacity = '0.8';
                // Add title tooltip
                nodeEl.setAttribute('title', 'Click to navigate');
              });

              nodeGroup.addEventListener('mouseleave', () => {
                nodeEl.style.opacity = '1';
              });

              // Mark as processed
              nodeGroup.setAttribute('data-clickable', 'true');
            }
          });
        } catch (e) {
          // Invalid selector, skip
        }
      });
    });

    // Fallback: Try to match by text content for nodes we couldn't match by ID
    nodeGroups.forEach((nodeGroup) => {
      if (nodeGroup.getAttribute('data-clickable')) {
        return; // Already processed
      }

      const textElements = nodeGroup.querySelectorAll('text');
      let textContent = '';
      textElements.forEach((text) => {
        textContent += (text.textContent || '') + ' ';
      });
      textContent = textContent.toLowerCase().trim();

      // Try to match flow nodes (contain "flow" and have a name)
      if (textContent.includes('flow') && !textContent.includes('policy')) {
        // Extract flow index from node ID or try to match
        const nodeIdAttr = nodeGroup.getAttribute('id') || '';

        // Check for API flows (Flow0, Flow1, etc.)
        for (const [nodeId, handler] of this.nodeClickHandlers.entries()) {
          if (nodeId.startsWith('Flow') && (nodeIdAttr.includes(nodeId.toLowerCase()) || textContent.includes(nodeId.toLowerCase()))) {
            const nodeEl = nodeGroup as HTMLElement;
            nodeEl.style.cursor = 'pointer';

            nodeGroup.addEventListener('click', (e) => {
              e.preventDefault();
              e.stopPropagation();
              handler();
            });

            nodeGroup.addEventListener('mouseenter', () => {
              nodeEl.style.opacity = '0.8';
              nodeEl.setAttribute('title', 'Click to open in Policy Studio');
            });

            nodeGroup.addEventListener('mouseleave', () => {
              nodeEl.style.opacity = '1';
            });

            nodeGroup.setAttribute('data-clickable', 'true');
            break;
          }
        }
      }

      // Try to match plan nodes
      if (textContent.includes('status:') && textContent.includes('mode:')) {
        for (const [nodeId, handler] of this.nodeClickHandlers.entries()) {
          if (nodeId.startsWith('Plan') && !nodeId.includes('Flow')) {
            const nodeIdAttr = nodeGroup.getAttribute('id') || '';
            if (nodeIdAttr.includes(nodeId.toLowerCase()) || textContent.includes('plan')) {
              const nodeEl = nodeGroup as HTMLElement;
              nodeEl.style.cursor = 'pointer';

              nodeGroup.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                handler();
              });

              nodeGroup.addEventListener('mouseenter', () => {
                nodeEl.style.opacity = '0.8';
                nodeEl.setAttribute('title', 'Click to edit plan');
              });

              nodeGroup.addEventListener('mouseleave', () => {
                nodeEl.style.opacity = '1';
              });

              nodeGroup.setAttribute('data-clickable', 'true');
              break;
            }
          }
        }
      }
    });
  }

  onSubmit() {
    const apiDetailsFormValue = this.apiDetailsForm.getRawValue();
    const apiImagesFormValue = this.apiImagesForm.getRawValue();

    return this.apiService
      .get(this.apiId)
      .pipe(
        map((api: Api) => {
          if (api.definitionVersion === 'V2') {
            const apiToUpdate: UpdateApiV2 = {
              ...(api as ApiV2),
              name: apiDetailsFormValue.name,
              apiVersion: apiDetailsFormValue.version,
              description: apiDetailsFormValue.description,
              labels: apiDetailsFormValue.labels,
              categories: apiDetailsFormValue.categories,
              ...(this.canDisplayV4EmulationEngineToggle
                ? { executionMode: apiDetailsFormValue.emulateV4Engine ? 'V4_EMULATION_ENGINE' : 'V3' }
                : {}),
            };
            return apiToUpdate;
          }
          const apiToUpdate: UpdateApiV4 = {
            ...(api as ApiV4),
            name: apiDetailsFormValue.name,
            apiVersion: apiDetailsFormValue.version,
            description: apiDetailsFormValue.description,
            labels: apiDetailsFormValue.labels,
            categories: apiDetailsFormValue.categories,
          };
          return apiToUpdate;
        }),
        switchMap((api: UpdateApi) => {
          if (this.apiDetailsForm.dirty) {
            return this.apiService.update(this.apiId, api);
          }
          return of(this.api);
        }),
        switchMap((api: Api) => {
          if (this.apiImagesForm.controls['picture'].dirty) {
            const picture = getBase64(apiImagesFormValue.picture[0]);
            if (picture) {
              return this.apiService.updatePicture(this.apiId, picture).pipe(switchMap(() => of(api)));
            }
            return this.apiService.deletePicture(this.apiId).pipe(switchMap(() => of(api)));
          }
          return of(api);
        }),
        switchMap((api: Api) => {
          if (this.apiImagesForm.controls['background'].dirty) {
            const background = getBase64(apiImagesFormValue.background[0]);
            if (background) {
              return this.apiService.updateBackground(this.apiId, background).pipe(switchMap(() => of(api)));
            }
            return this.apiService.deleteBackground(this.apiId).pipe(switchMap(() => of(api)));
          }
          return of(api);
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        tap(() => {
          this.apiId = undefined; // force to reload quality metrics
          this.ngOnInit();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  importApi() {
    this.policyService
      .listSwaggerPolicies()
      .pipe(
        switchMap((policies) =>
          this.matDialog
            .open<GioApiImportDialogComponent, GioApiImportDialogData>(GioApiImportDialogComponent, {
              data: {
                apiId: this.apiId,
                policies,
              },
              role: 'alertdialog',
              id: 'importApiDialog',
            })
            .afterClosed(),
        ),
        filter((apiId) => !!apiId),
        tap(() => {
          this.refresh$.next();
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? 'An error occurred while importing the API.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  duplicateApi() {
    this.matDialog
      .open<ApiGeneralInfoDuplicateDialogComponent, ApiPortalDetailsDuplicateDialogData>(ApiGeneralInfoDuplicateDialogComponent, {
        data: {
          api: this.api,
        },
        role: 'alertdialog',
        id: 'duplicateApiDialog',
      })
      .afterClosed()
      .pipe(
        filter((apiDuplicated) => !!apiDuplicated),
        switchMap((apiDuplicated) => this.router.navigate(['../', apiDuplicated.id], { relativeTo: this.activatedRoute })),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  exportApi() {
    const exportDialog$ =
      this.api.definitionVersion === 'V4'
        ? this.matDialog
            .open<ApiGeneralInfoExportV4DialogComponent, ApiGeneralDetailsExportV4DialogData, ApiGeneralDetailsExportV4DialogResult>(
              ApiGeneralInfoExportV4DialogComponent,
              {
                data: {
                  api: this.api,
                },
                role: 'alertdialog',
                id: 'exportApiDialog',
              },
            )
            .afterClosed()
        : this.matDialog
            .open<ApiGeneralInfoExportV2DialogComponent, ApiPortalDetailsExportV2DialogData>(ApiGeneralInfoExportV2DialogComponent, {
              data: {
                api: this.api,
              },
              role: 'alertdialog',
              id: 'exportApiDialog',
            })
            .afterClosed();

    exportDialog$.pipe(takeUntil(this.unsubscribe$)).subscribe();
  }

  promoteApi() {
    if (!this.cannotPromote)
      this.matDialog
        .open<ApiGeneralInfoPromoteDialogComponent, ApiPortalDetailsPromoteDialogData>(ApiGeneralInfoPromoteDialogComponent, {
          data: {
            api: this.api,
          },
          role: 'alertdialog',
          id: 'promoteApiDialog',
        })
        .afterClosed()
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe();
  }

  migrateToV4() {
    this.matDialog
      .open(ApiGeneralInfoMigrateToV4DialogComponent, { data: { apiId: this.apiId }, width: GIO_DIALOG_WIDTH.SMALL })
      .afterClosed()
      .pipe(
        filter((result): result is MigrateDialogResult => !!result?.confirmed),
        switchMap((result) => {
          if (result.state !== 'MIGRATABLE' && result.state !== 'CAN_BE_FORCED') {
            return throwError(() => new Error(`Unexpected migration state received: ${result.state}`));
          }
          if (result.state === 'CAN_BE_FORCED') {
            return this.apiService.migrateToV4(this.apiId, 'FORCE');
          }
          return this.apiService.migrateToV4(this.apiId);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          const successMessage = 'API Migrated to v4 successfully!\nYou can now deploy the migrated API when ready.';
          this.snackBarService.success(successMessage);
          this.ngOnInit();
        },
        error: (err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
        },
      });
  }

  private canChangeApiLifecycle(api: Api): boolean {
    if (this.constants.env?.settings?.apiReview?.enabled) {
      return !api.workflowState || api.workflowState === 'REVIEW_OK';
    } else {
      return api.lifecycleState === 'CREATED' || api.lifecycleState === 'PUBLISHED' || api.lifecycleState === 'UNPUBLISHED';
    }
  }
}

const isImgUrl = (url: string): Promise<boolean> => {
  const img = new Image();
  img.src = url;
  return new Promise((resolve) => {
    img.onerror = () => resolve(false);
    img.onload = () => resolve(true);
  });
};

function getBase64(file?: NewFile | string): string | undefined | null {
  if (!file) {
    // If no file, return null to remove it
    return null;
  }
  if (!(file instanceof NewFile)) {
    // If file not changed, return undefined to keep it
    return undefined;
  }

  return file.dataUrl;
}
