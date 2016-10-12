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
  constructor($scope, $state, $stateParams, $window, $q, $timeout, ApiService, NotificationService) {
    'ngInject';
    this.$scope = $scope;
    this.$state = $state;
    this.$stateParams = $stateParams;
    this.$window = $window;
    this.$q = $q;
    this.$timeout = $timeout;
    this.ApiService = ApiService;
    this.NotificationService = NotificationService;

    this.api = _.clone(this.$stateParams.api);

    this.vm = {};

    this.vm.selectedStep = 0;
    this.vm.stepProgress = 1;
    this.vm.maxStep = 2;
    this.vm.showBusyText = false;
    this.vm.stepData = [
      {step: 1, completed: false, optional: false, data: {}},
      {step: 2, completed: false, optional: false, data: {}}];
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
    this.vm.selectedStep = this.vm.selectedStep + 1;
  }

  moveToPreviousStep() {
    if (this.vm.selectedStep > 0) {
      this.vm.selectedStep = this.vm.selectedStep - 1;
    }
  }

  submitCurrentStep(stepData) {
    this.vm.showBusyText = true;

    if (!stepData.completed) {
      var _this = this;

      if (this.vm.selectedStep != 1) {
        _this.vm.showBusyText = false;
        //move to next step when success
        stepData.completed = true;
        _this.enableNextStep();
      } else {
        this.ApiService.create(this.api).then(function (api) {
          _this.vm.showBusyText = false;
          _this.NotificationService.show('API created');
          _this.$window.location.href = '#/apis/' + api.data.id + '/settings/general';
        }).catch(function (error) {
          _this.vm.showBusyText = false;
        });
      }
    } else {
      this.vm.showBusyText = false;
      this.enableNextStep();
    }
  }
}

export default NewApiController;
