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
class ApiDiscoveryController {
  private api: any;
  private discovery: any;

  constructor(
    private ApiService,
    private NotificationService,
    private $scope,
    private $rootScope
  ) {
    'ngInject';
    this.api = this.$scope.$parent.apiCtrl.api;
    this.discovery = this.api.services && this.api.services['discovery'];
    this.discovery = this.discovery || {enabled: false, configuration: {}};
  }

  update() {
      // Discovery is disabled, set dummy values
      if (this.discovery.enabled === false) {
        delete this.discovery.configuration;
      } else {
        // Set default provider
        this.discovery.provider = 'CONSUL';
      }
      this.api.services['discovery'] = this.discovery;

    this.ApiService.update(this.api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.$scope.formApiDiscoveryConfiguration.$setPristine();
      this.$rootScope.$broadcast('apiChangeSuccess', {api: this.api});

      this.NotificationService.show('Endpoint discovery configuration has been updated');
    });
  }
}
export default ApiDiscoveryController;
