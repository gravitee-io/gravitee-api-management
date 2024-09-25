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
import { IPromise } from 'angular';

import { ActivatedRoute, Router } from '@angular/router';
import { forEach, remove, some } from 'lodash';

import { ApiService } from '../../../../services/api.service';
import NotificationService from '../../../../services/notification.service';
import UserService from '../../../../services/user.service';
import { PlanSecurityType } from '../../../../entities/plan';
import { IfMatchEtagInterceptor } from '../../../../shared/interceptors/if-match-etag.interceptor';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

interface Page {
  fileName: string;
  published?: boolean;
  name?: string;
}

interface Api {
  name?: string;
  version?: string;
  gravitee: string;
  proxy: {
    endpoints: any[];
    context_path?: string;
  };
  pages: Array<Page>;
  plans: any[];
  tags: any[];
  groups: any[];
  lifecycle_state?: string;
  execution_mode?: string;
}

class ApiCreationV2ControllerAjs {
  api: Api;
  selectedTenants: any[];
  attachableGroups: any[];
  poGroups: any[];
  activatedRoute: ActivatedRoute;
  isCreating: boolean;

  private vm: {
    selectedStep: number;
    stepProgress: number;
    maxStep: number;
    showBusyText: boolean;
    stepData: {
      step: number;
      label?: string;
      completed: boolean;
      optional: boolean;
      data: any;
    }[];
  };
  private contextPathInvalid: boolean;
  private plan: any;

  private pages: any;
  private securityTypes: { id: string; name: string }[];
  private rateLimitTimeUnits: string[];
  private quotaTimeUnits: string[];
  private methods: string[];
  private resourceFiltering: {
    whitelist: any;
  };
  private skippedStep: boolean;
  private endpoint: any;
  private rateLimit: any;
  private quota: any;

  // Inject with component binding & route resolver
  // Useful in template of steps
  private tags: any[];
  private tenants: any[];
  private groups: any[];

  constructor(
    private $scope,
    private $timeout,
    private $mdDialog,
    private $window,
    private ApiService: ApiService,
    private ngApiV2Service: ApiV2Service,
    private NotificationService: NotificationService,
    private UserService: UserService,
    private Constants: any,
    private $rootScope,
    private readonly ngIfMatchEtagInterceptor: IfMatchEtagInterceptor,
    private readonly ngRouter: Router,
  ) {
    this.api = {
      gravitee: '2.0.0',
      proxy: {
        endpoints: [],
      },
      pages: [],
      plans: [],
      tags: [],
      groups: [],
    };

    if (this.Constants.org.settings.v4EmulationEngine.defaultValue !== 'no') {
      this.api.execution_mode = 'v4-emulation-engine';
    } else {
      this.api.execution_mode = 'v3';
    }

    this.contextPathInvalid = true;
    this.plan = {
      characteristics: [],
    };

    this.pages = {};
    this.securityTypes = [];
    if (this.Constants.env.settings.plan.security.apikey.enabled) {
      this.securityTypes.push({
        id: PlanSecurityType.API_KEY,
        name: 'API Key',
      });
    }
    if (this.Constants.env.settings.plan.security.keyless.enabled) {
      this.securityTypes.push({
        id: PlanSecurityType.KEY_LESS,
        name: 'Keyless (public)',
      });
    }

    this.rateLimitTimeUnits = ['SECONDS', 'MINUTES'];
    this.quotaTimeUnits = ['HOURS', 'DAYS', 'WEEKS', 'MONTHS'];

    this.methods = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'];

    this.resourceFiltering = {
      whitelist: [],
    };

    // init steps settings
    this.initStepSettings();

    // init documentation settings
    this.initDocumentationSettings();
  }

  $onInit = () => {
    this.attachableGroups = this.groups.filter((group) => group.apiPrimaryOwner == null);
    const currentUserGroups = this.UserService.getCurrentUserGroups();
    this.poGroups = this.groups.filter(
      (group) => group.apiPrimaryOwner != null && currentUserGroups.some((userGroup) => userGroup === group.name),
    );
  };

  /*
   md-stepper
   */
  initStepSettings() {
    this.skippedStep = false;
    this.vm = {
      selectedStep: 0,
      stepProgress: 1,
      maxStep: 5,
      showBusyText: false,
      stepData: [
        { step: 1, completed: false, optional: false, data: {} },
        { step: 2, completed: false, optional: false, data: {} },
        { step: 3, label: 'Plan', completed: false, optional: true, data: {} },
        { step: 4, label: 'Documentation', completed: false, optional: true, data: {} },
        { step: 5, label: 'Confirmation', completed: false, optional: false, data: {} },
      ],
    };
  }

  enableNextStep() {
    // do not exceed into max step
    if (this.vm.selectedStep >= this.vm.maxStep) {
      return;
    }
    // do not increment vm.stepProgress when submitting from previously completed step
    if (this.vm.selectedStep === this.vm.stepProgress - 1) {
      this.vm.stepProgress = this.vm.stepProgress + 1;
    }

    // change api step state
    if (this.skippedStep) {
      this.skippedStep = false;
    }
    this.$timeout(() => {
      this.vm.selectedStep = this.vm.selectedStep + 1;
    });
  }

  moveToPreviousStep() {
    if (this.vm.selectedStep > 0) {
      this.vm.selectedStep = this.vm.selectedStep - 1;
    } else {
      this.ngRouter.navigate(['..'], { relativeTo: this.activatedRoute });
    }
  }

  selectStep(step) {
    this.vm.selectedStep = step;
  }

  submitCurrentStep(stepData) {
    this.vm.showBusyText = true;
    if (!stepData.completed) {
      if (this.vm.selectedStep !== 4) {
        this.vm.showBusyText = false;
        // move to next step when success
        stepData.completed = true;
        this.enableNextStep();
      }
    } else {
      this.vm.showBusyText = false;
      this.enableNextStep();
    }
  }

  /*
   API creation
   */
  createAPI(deployAndStart, readyForReview?: boolean) {
    if (!this.isCreating) {
      this.isCreating = true;
      // clear API pages json format
      forEach(this.api.pages, (page) => {
        if (!page.name) {
          page.name = page.fileName;
        }
        delete page.fileName;
        // handle publish state
        page.published = deployAndStart;
      });

      // handle plan publish state
      forEach(this.api.plans, (plan) => {
        plan.status = deployAndStart ? 'PUBLISHED' : 'STAGING';
      });

      if (this.api.groups != null) {
        this.api.groups = this.api.groups.map((group) => group.name);
      }

      // create API
      if (deployAndStart) {
        this.api.lifecycle_state = 'PUBLISHED';
      }

      this.ApiService.import(null, this.api, this.api.gravitee, false)
        .then((api) => {
          this.vm.showBusyText = false;
          return api;
        })
        .then((api) => {
          if (readyForReview) {
            this.ApiService.askForReview(api.data).then(() => {
              api.data.workflow_state = 'IN_REVIEW';
              this.api = api.data;
            });
          }
          return api;
        })
        .then((api) => {
          if (deployAndStart) {
            this.ApiService.deploy(api.data.id).then(() => {
              this.ApiService.start(api.data).then(() => {
                this.NotificationService.show('API created, deployed and started');
                this.ngRouter.navigate(['../..', api.data.id], { relativeTo: this.activatedRoute });
              });
            });
          } else {
            this.NotificationService.show('API created');
            this.ngRouter.navigate(['../..', api.data.id], { relativeTo: this.activatedRoute });
          }

          return api;
        })
        .then((api) => this.ngApiV2Service.get(api.data.id).toPromise())
        .catch(() => {
          this.vm.showBusyText = false;
          this.isCreating = false;
        });
    }
  }

  /*
   API context-path
   */
  validFirstStep(stepData) {
    if (this.contextPathInvalid) {
      const pathToVerify = [{ path: this.api.proxy.context_path }];
      this.ngApiV2Service.verifyPath(null, pathToVerify).subscribe(
        (res) => {
          this.contextPathInvalid = !res.ok;
          if (this.contextPathInvalid) {
            this.NotificationService.show(`Invalid context path ${res.reason}`);
          } else {
            this.submitCurrentStep(stepData);
          }
        },
        () => {
          this.contextPathInvalid = true;
        },
      );
    } else {
      this.submitCurrentStep(stepData);
    }
  }

  onChangeContextPath() {
    this.contextPathInvalid = true;
  }

  /*
   API endpoint
   */
  selectEndpoint() {
    this.api.proxy.endpoints = [];
    const endpoint = {
      name: 'default',
      target: this.endpoint,
      tenants: this.selectedTenants,
      inherit: true,
    };

    this.api.proxy.endpoints.push(endpoint);
  }

  /*
   API plan
   */
  selectPlan() {
    // set validation mode
    if (this.plan.security === PlanSecurityType.KEY_LESS) {
      this.plan.validation = 'AUTO';
    }
    if (!this.plan.validation) {
      this.plan.validation = 'MANUAL';
    }

    // set resource filtering whitelist
    remove(this.resourceFiltering.whitelist, (whitelistItem: any) => {
      return !whitelistItem.pattern;
    });
    if (this.api.gravitee === '1.0.0') {
      this.plan.paths = {
        '/': [],
      };

      if (this.resourceFiltering.whitelist.length) {
        this.plan.paths['/'].push({
          methods: this.methods,
          'resource-filtering': {
            whitelist: this.resourceFiltering.whitelist,
          },
        });
      }
      // set rate limit policy
      if (this.rateLimit && this.rateLimit.limit) {
        this.plan.paths['/'].push({
          methods: this.methods,
          'rate-limit': {
            rate: this.rateLimit,
          },
        });
      }
      // set quota policy
      if (this.quota && this.quota.limit) {
        this.plan.paths['/'].push({
          methods: this.methods,
          quota: {
            quota: this.quota,
            addHeaders: true,
          },
        });
      }
    } else {
      const flow = {
        'path-operator': {
          path: '/',
          operator: 'STARTS_WITH',
        },
        condition: '',
        pre: [],
        post: [],
      };
      if (this.resourceFiltering.whitelist.length) {
        flow.pre.push({
          name: 'Resource Filtering',
          policy: 'resource-filtering',
          configuration: {
            whitelist: this.resourceFiltering.whitelist,
          },
        });
      }
      // set rate limit policy
      if (this.rateLimit && this.rateLimit.limit) {
        flow.pre.push({
          name: 'Rate limit',
          policy: 'rate-limit',
          configuration: {
            rate: this.rateLimit,
          },
        });
      }
      // set quota policy
      if (this.quota && this.quota.limit) {
        flow.pre.push({
          name: 'Quota',
          policy: 'quota',
          configuration: {
            quota: this.quota,
            addHeaders: true,
          },
        });
      }
      this.plan.flows = [flow];
    }
    this.api.plans = [this.plan];
  }

  skipAddPlan() {
    this.api.plans = [];
    this.plan = {};
    this.skippedStep = true;
  }

  resetRateLimit() {
    delete this.rateLimit;
  }

  resetQuota() {
    delete this.quota;
  }

  /*
   API documentation
   */
  initDocumentationSettings() {
    this.$scope.$watch('newApiPageFile.content', (data) => {
      if (data) {
        const file = {
          name: this.$scope.newApiPageFile.name,
          content: data,
          type: '',
        };

        const fileExtension = file.name.split('.').pop().toUpperCase();
        switch (fileExtension) {
          case 'MD':
            file.type = 'MARKDOWN';
            break;
          case 'YAML':
          case 'YML':
          case 'JSON':
            if (file.content.match(/.*"?(swagger|openapi)"?: *['"]?\d/)) {
              file.type = 'SWAGGER';
            } else if (file.content.match(/.*"?asyncapi"?: *['"]?\d/)) {
              file.type = 'ASYNCAPI';
            }
            break;
          case 'ADOC':
            file.type = 'ASCIIDOC';
            break;
        }
        if (file.type) {
          this.selectFile(file);
        } else {
          this.NotificationService.showError('Only Markdown, OpenAPI, AsyncAPI, and AsciiDoc files are supported');
        }
      }
    });
  }

  selectFile(file) {
    if (file && !this.pageAlreadyExist(file.name)) {
      const page = {
        fileName: file.name,
        name: file.name,
        content: file.content,
        type: file.type,
        published: false,
      };

      this.api.pages.push(page);
    }
  }

  pageAlreadyExist(pageFileName) {
    return some(this.api.pages, (page: any) => {
      return page.fileName === pageFileName;
    });
  }

  hasPage() {
    return this.api.pages && this.api.pages.length > 0;
  }

  removePage(pageToRemove: { fileName: string }): IPromise<void> {
    return this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('html-loader!../../../../components/dialog/confirmWarning.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
        clickOutsideToClose: true,
        locals: {
          title: 'Warning',
          msg: 'Are you sure you want to remove this page?',
        },
      })
      .then(() => {
        this.api.pages = this.api.pages.filter((page) => page.fileName !== pageToRemove.fileName);
      });
  }

  skipDocumentation() {
    this.api.pages = [];
    this.skippedStep = true;
  }
}
ApiCreationV2ControllerAjs.$inject = [
  '$scope',
  '$timeout',
  '$mdDialog',
  '$window',
  'ApiService',
  'ngApiV2Service',
  'NotificationService',
  'UserService',
  'Constants',
  '$rootScope',
  'ngIfMatchEtagInterceptor',
  'ngRouter',
];

export default ApiCreationV2ControllerAjs;
