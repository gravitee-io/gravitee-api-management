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
class NewApiController {
  constructor($scope, $state, $stateParams, $window, $q, base64, $mdDialog, ApiService, NotificationService, DocumentationService, $timeout) {
    'ngInject';
    this.$scope = $scope;
    this.$state = $state;
    this.$stateParams = $stateParams;
    this.$window = $window;
    this.$q = $q;
    this.base64 = base64;
    this.$mdDialog = $mdDialog;
    this.ApiService = ApiService;
    this.NotificationService = NotificationService;
    this.DocumentationService = DocumentationService;
    this.$timeout = $timeout;

    this.api = _.clone(this.$stateParams.api) !== null ? _.clone(this.$stateParams.api) : {};
    this.contextPathInvalid = true;
    this.api.proxy = {};
    this.api.proxy.endpoints = [];
    this.api.pages = [];
    this.api.plans = [];
    this.plan = {};
    this.plan.characteristics = [];
    this.pages = {};
    this.securityTypes = [
      {
        'id': 'API_KEY',
        'name': 'API Key'
      }, {
        'id': 'KEY_LESS',
        'name': 'Keyless (public)'
      }];
    this.timeUnits = ['SECONDS', 'MINUTES', 'HOURS', 'DAYS'];
    this.methods = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'];
    this.resourceFiltering = {};
    this.resourceFiltering.whitelist = [];

    // init steps settings
    this.initStepSettings();

    // init import API settings
    this.initImportAPISettings();

    // init documentation settings
    this.initDocumentationSettings();
  }

  /*
    md-stepper
   */
  initStepSettings() {
    this.vm = {};
    this.vm.selectedStep = 0;
    this.vm.stepProgress = 1;
    this.vm.maxStep = 5;
    this.vm.showBusyText = false;
    this.vm.stepData = [
      {step: 1, label: "General", completed: false, optional: false, data: {}},
      {step: 2, label: "Gateway", completed: false, optional: false, data: {}},
      {step: 3, label: "Plan", completed: false, optional: true, data: {}},
      {step: 4, label: "Documentation", completed: false, optional: true, data: {}},
      {step: 5, label: "Confirmation", completed: false, optional: false, data: {}}];

    this.skippedStep = false;
    this.apiSteps = [];
    this.apiSteps.push(this.steps()[0]);
  }

  enableNextStep() {
    //do not exceed into max step
    if (this.vm.selectedStep >= this.vm.maxStep) {
      return;
    }
    //do not increment vm.stepProgress when submitting from previously completed step
    if (this.vm.selectedStep === this.vm.stepProgress - 1) {
      this.vm.stepProgress = this.vm.stepProgress + 1;
    }

    //change api step state
    if (this.skippedStep) {
      this.apiSteps[this.vm.selectedStep].badgeClass = 'disable';
      this.apiSteps[this.vm.selectedStep].badgeIconClass = 'glyphicon-remove-circle';
      this.apiSteps[this.vm.selectedStep].title = this.steps()[this.vm.selectedStep].title + " <em>skipped</em>";
      this.skippedStep = false;
    } else {
      this.apiSteps[this.vm.selectedStep].badgeClass = 'info';
      this.apiSteps[this.vm.selectedStep].badgeIconClass = 'glyphicon-ok-circle';
    }
    if (!this.apiSteps[this.vm.selectedStep + 1]) {
      this.apiSteps.push(this.steps()[this.vm.selectedStep + 1]);
    }

    var that = this;
    this.$timeout(function () {
      that.vm.selectedStep = that.vm.selectedStep + 1;
    });
  }

  moveToPreviousStep() {
    if (this.vm.selectedStep > 0) {
      this.vm.selectedStep = this.vm.selectedStep - 1;
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
        //move to next step when success
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
  createAPI(deployAndStart) {
    var alert = this.$mdDialog.confirm({
      title: 'Create API ?',
      content: 'The API ' + this.api.name + ' in version ' + this.api.version + ' will be create' + ((deployAndStart) ? ' and deploy.' : '.'),
      ok: 'CREATE',
      cancel: 'CANCEL'
    });

    var that = this;
    this.$mdDialog
      .show(alert)
      .then(function () {
        that._createAPI(deployAndStart);
      });
  }

  _createAPI(deployAndStart) {
    var _this = this;
    // clear API pages json format
    _.forEach(this.api.pages, function(page) {
      if (!page.name) {
        page.name = page.fileName;
      }
      delete page.fileName;
      // handle publish state
      page.published = deployAndStart;
    });

    // handle plan publish state
    _.forEach(this.api.plans, function(plan) {
      plan.status = (deployAndStart) ? 'PUBLISHED' : 'STAGING';
    });

    // create API
    this.ApiService.import(null, this.api).then(function (api) {
      _this.vm.showBusyText = false;
      if (deployAndStart) {
        _this.ApiService.deploy(api.data.id).then(function() {
          _this.ApiService.start(api.data.id).then(function() {
            _this.NotificationService.show('API created, deployed and started');
            _this.$window.location.href = '#/apis/' + api.data.id + '/settings/general';
          });
        });
      } else {
        _this.NotificationService.show('API created');
        _this.$window.location.href = '#/apis/' + api.data.id + '/settings/general';
      }
    }).catch(function () {
      _this.vm.showBusyText = false;
    });
  }

  /*
    API import
   */
  initImportAPISettings() {
    var that = this;
    this.enableFileImport = false;
    this.importFileMode= true;
    this.importURLMode= false;
    this.apiDescriptorURL = null;
    this.$scope.$watch('importAPIFile.content', function (data) {
      if (data) {
        that.enableFileImport = true;
      }
    });
  }

  importAPI() {
    if (this.importFileMode) {
      var extension = this.$scope.importAPIFile.name.split('.').pop();
      switch (extension) {
        case "yml" :
        case "yaml" :
          this.importSwagger();
          break;
        case "json" :
          if (this.isSwaggerDescriptor()) {
            this.importSwagger();
          } else {
            this.importGraviteeIODefinition();
          }
          break;
        default:
          this.enableFileImport = false;
          this.NotificationService.showError("Input file must be a valid API definition file.");
      }
    } else {
      this.importSwagger();
    }
  }

  importGraviteeIODefinition() {
    var _this = this;
    this.ApiService.import(null, this.$scope.importAPIFile.content).then(function (api) {
      _this.NotificationService.show('API created');
      _this.$window.location.href = '#/apis/' + api.data.id + '/settings/general';
    });
  }

  importSwagger() {
    var _this = this;
    var swagger = { version: 'VERSION_2_0' };
    if (this.importFileMode) {
      swagger.type = 'INLINE';
      swagger.payload = this.$scope.importAPIFile.content;
    } else {
      swagger.type = 'URL';
      swagger.payload = this.apiDescriptorURL;
    }

    this.ApiService.importSwagger(swagger).then(function (api) {
      var importedAPI = api.data;
      importedAPI.contextPath = importedAPI.name.replace(/\s+/g, '').toLowerCase();
      importedAPI.description = (importedAPI.description) ? importedAPI.description : "Default API description";
      _this.ApiService.create(importedAPI).then(function(api) {
        _this.NotificationService.show('API created');
        _this.$window.location.href = '#/apis/' + api.data.id + '/settings/general';
      });
    });
  }

  enableImport() {
    if (this.importFileMode) {
      return this.enableFileImport;
    } else {
      return (this.apiDescriptorURL && this.apiDescriptorURL.length);
    }
  }

  isSwaggerDescriptor() {
    try {
      var fileContent = JSON.parse(this.$scope.importAPIFile.content);
      return fileContent.hasOwnProperty('swagger');
    } catch (e) {
      this.NotificationService.showError("Invalid json file.");
    }
  }

  /*
    API context-path
   */
  validFirstStep(stepData) {
    var stepMessage = this.api.name + " (" + this.api.version + ") <code>" + this.api.proxy.context_path + "</code>";
    if (this.contextPathInvalid) {
      var _this = this;
      var criteria = { 'context_path' : this.api.proxy.context_path};
      this.ApiService.verify(criteria).then(function () {
        _this.contextPathInvalid = false;
        _this.submitCurrentStep(stepData);
        _this.apiSteps[_this.vm.selectedStep].title = stepMessage;
      }, function () {
        _this.contextPathInvalid = true;
      });
    } else {
      this.submitCurrentStep(stepData);
      this.apiSteps[this.vm.selectedStep].title = stepMessage;
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
    var endpoint = {};
    endpoint.name = "default";
    endpoint.target = this.endpoint;
    this.api.proxy.endpoints.push(endpoint);

    // set api step message
    var stepMessage = endpoint.target;
    this.apiSteps[this.vm.selectedStep].title = stepMessage;
  }

  /*
    API plan
   */
  selectPlan() {
    // set validation mode
    if (this.plan.security === 'KEY_LESS') {
      this.plan.validation = 'AUTO';
    }
    if (!this.plan.validation) {
      this.plan.validation = 'MANUAL';
    }
    this.api.plans = [];
    this.plan.paths = {
      '/': []
    };
    // set resource filtering whitelist
    _.remove(this.resourceFiltering.whitelist, function (whitelistItem) {
      return !whitelistItem.pattern;
    });
    if (this.resourceFiltering.whitelist.length) {
      this.plan.paths['/'].push({
        'methods': this.methods,
        'resource-filtering': {
          'whitelist': this.resourceFiltering.whitelist
        }
      });
    }
    // set rate limit policy
    if (this.rateLimit && this.rateLimit.limit) {
      this.plan.paths['/'].push({
        'methods': this.methods,
        'rate-limit': {
          'rate': this.rateLimit
        }
      });
    }
    // set quota policy
    if (this.quota && this.quota.limit) {
      this.plan.paths['/'].push({
        'methods': this.methods,
        'quota': {
          'quota': this.quota,
          'addHeaders': true
        }
      });
    }
    this.api.plans.push(this.plan);
    // set api step message
    var stepMessage = this.plan.name + " <code>"+this.plan.security+ "</code> <code>"+this.plan.validation+"</code>";
    this.apiSteps[this.vm.selectedStep].title = stepMessage;
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
    var that = this;
    this.$scope.$watch('newApiPageFile.content', function (data) {
      if (data) {
        var file = {};
        file.name = that.$scope.newApiPageFile.name;
        file.content = data;
        var fileExtension = file.name.split('.').pop().toUpperCase();
        switch (fileExtension) {
          case "MD" :
            file.type = 'MARKDOWN';
            break;
          case "RAML" :
            file.type = 'RAML';
            break;
          case "YAML" :
          case "YML" :
          case "JSON" :
            file.type = 'SWAGGER';
            break;
        }
        if (file.type) {
          that.selectFile(file);
        } else {
          that.NotificationService.showError("Only Markdown, Swagger and Raml file are supported");
        }
      }
    });
  }

  selectDocumentation() {
    var stepMessage = "";
    _.forEach(this.api.pages, function(page) {
      stepMessage += page.name + " ";
    });
    this.apiSteps[this.vm.selectedStep].title = stepMessage;
  }

  selectFile(file) {
    if (file && !this.pageAlreadyExist(file.name)) {
      var page = {};
      page.fileName = file.name;
      page.name = file.name;
      page.content = file.content;
      page.type = file.type;
      page.published = false;
      this.api.pages.push(page);
    }
  }

  pageAlreadyExist(pageFileName) {
    return _.some(this.api.pages, function (page) {
      return page.fileName === pageFileName;
    });
  }

  hasPage() {
    return this.api.pages.length > 0;
  }

  removePage(page) {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to remove this page ?',
      ok: 'OK',
      cancel: 'Cancel'
    });

    var that = this;
    this.$mdDialog
      .show(alert)
      .then(function () {
        _.remove(that.api.pages, function (_page) {
          return _page.fileName === page.fileName;
        });
      });
  }

  skipDocumentation() {
    this.api.pages = [];
    this.skippedStep = true;
  }

  steps() {
    return [{
      badgeClass: 'disable',
      badgeIconClass: 'glyphicon-refresh',
      title: 'General',
      content: 'Name, version and context-path'
    }, {
      badgeClass: 'disable',
      badgeIconClass: 'glyphicon-refresh',
      title: 'Gateway',
      content: 'Endpoint'
    }, {
      badgeClass: 'disable',
      badgeIconClass: 'glyphicon-refresh',
      title: 'Plan',
      content: 'Name, security type and validation mode'
    }, {
      badgeClass: 'disable',
      badgeIconClass: 'glyphicon-refresh',
      title: 'Documentation',
      content: 'Pages name'
    }];
  }
}

export default NewApiController;
