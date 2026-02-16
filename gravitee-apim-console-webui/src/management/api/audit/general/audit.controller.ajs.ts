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

import { ActivatedRoute } from '@angular/router';

import AuditService from '../../../../services/audit.service';

class ApiAuditControllerAjs {
  activatedRoute: ActivatedRoute;
  constructor(
    private AuditService: AuditService,
    private $scope,
  ) {}

  $onInit() {
    this.$scope.api = this.activatedRoute.snapshot.params.apiId;
    this.$scope.apis = [{ id: this.activatedRoute.snapshot.params.apiId }];
    this.$scope.applications = [];
    this.AuditService.listEvents(this.activatedRoute.snapshot.params.apiId).then(response => {
      this.$scope.events = response.data;
    });
  }
}
ApiAuditControllerAjs.$inject = ['AuditService', '$scope'];

export default ApiAuditControllerAjs;
