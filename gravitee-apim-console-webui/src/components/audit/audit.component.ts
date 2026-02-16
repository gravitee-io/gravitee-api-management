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
import { forEach, map, mapValues } from 'lodash';

import { AuditQuery, default as AuditService } from '../../services/audit.service';

const AuditComponent: ng.IComponentOptions = {
  template: require('html-loader!./audit.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    api: '<',
    apis: '<',
    applications: '<',
    events: '<',
  },
  controller: [
    'AuditService',
    function (AuditService: AuditService) {
      this.$onInit = () => {
        this.events = map(this.events, (ev: string) => {
          return ev.toUpperCase();
        });
        this.query = new AuditQuery();
        this.onPaginate = this.onPaginate.bind(this);
        AuditService.list(null, this.api).then(response => this.handleAuditResponseData(response.data));
        this.queryLogType = 'all';
      };

      this.handleAuditResponseData = responseData => {
        this.auditLogs = responseData.content;
        this.metadata = responseData.metadata;
        this.enhanceAuditLogs(this.auditLogs);
        this.query.page = responseData.pageNumber;
        this.result = {
          size: responseData.pageElements,
          total: responseData.totalElements,
        };
      };

      this.enhanceAuditLogs = auditLogs => {
        forEach(auditLogs, log => {
          log.prettyPatch = JSON.stringify(JSON.parse(log.patch), null, '  ');
          log.displayPatch = false;
          log.displayProperties = false;
        });
      };

      this.onPaginate = () => {
        AuditService.list(this.query, this.api).then(response => {
          this.handleAuditResponseData(response.data);
        });
      };

      this.getNameByReference = (ref: { type: string; id: string }) => {
        if (this.metadata[ref.type + ':' + ref.id + ':name']) {
          return this.metadata[ref.type + ':' + ref.id + ':name'];
        }
        return ref.id;
      };

      this.getDisplayableProperties = properties => {
        return mapValues(properties, (v, k) => this.metadata[k + ':' + v + ':name']);
      };

      this.onOrgEnvFilterChange = () => {
        if (this.queryLogType === 'env') {
          this.query.orgLog = false;
          this.query.envLog = true;
        } else if (this.queryLogType === 'org') {
          this.query.orgLog = true;
          this.query.envLog = false;
        } else {
          this.query.orgLog = false;
          this.query.envLog = false;
        }
      };

      this.search = () => {
        this.query.page = 1;
        if (this.query.mgmt) {
          this.query.api = null;
          this.query.application = null;
        }

        AuditService.list(this.query, this.api).then(response => {
          this.handleAuditResponseData(response.data);
        });
      };
    },
  ],
};

export default AuditComponent;
