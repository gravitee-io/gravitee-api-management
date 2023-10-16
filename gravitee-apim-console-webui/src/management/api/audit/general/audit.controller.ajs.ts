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

import AuditService from '../../../../services/audit.service';

class ApiAuditControllerAjs {
  constructor(private AuditService: AuditService, private $scope, private $state) {
    this.$scope.api = this.$state.params.apiId;
    this.$scope.apis = [{ id: this.$state.params.apiId }];
    this.$scope.applications = [];
  }

  $onInit() {
    this.AuditService.listEvents(this.$state.params.apiId).then((response) => {
      this.$scope.events = response.data;
    });
  }
}
ApiAuditControllerAjs.$inject = ['AuditService', '$scope', '$state'];

export default ApiAuditControllerAjs;
