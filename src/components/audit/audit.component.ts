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
import * as _ from 'lodash';
import {AuditQuery, default as AuditService} from "../../services/audit.service";

const AuditComponent: ng.IComponentOptions = {
  template: require('./audit.html'),
  bindings: {
    api: "<",
    apis: "<",
    applications: "<",
    events: "<"
  },
  controller: function(AuditService: AuditService){
    'ngInject';
    const vm = this;

    vm.$onInit = ()=> {
      vm.events = _.map(vm.events, (ev: string)=> {return ev.toUpperCase()});
      vm.query = new AuditQuery();
      vm.onPaginate = vm.onPaginate.bind(this);
      AuditService.list(null, vm.api).then(response =>
        vm.handleAuditResponseData(response.data)
      );
    };

    vm.handleAuditResponseData = (responseData)=> {
      vm.auditLogs = responseData.content;
      vm.metadata = responseData.metadata;
      vm.enhanceAuditLogs(vm.auditLogs);
      vm.query.page = responseData.pageNumber;
      vm.result = {
        size: responseData.pageElements,
        total: responseData.totalElements
      }
    };

    vm.enhanceAuditLogs = (auditLogs)=> {
      _.forEach(auditLogs, (log)=> {
          log.prettyPatch = JSON.stringify(JSON.parse(log.patch), null, '  ');
          log.displayPatch = false;
          log.displayProperties = false;
        }
      );
    };

    vm.onPaginate = ()=> {
      AuditService.list(vm.query, vm.api).then((response)=> {
        vm.handleAuditResponseData(response.data);
      });
    };

    vm.getNameByReference = ( ref: {type: string, id:string} )=> {
      if (vm.metadata[ref.type+':'+ref.id+':name']) {
        return vm.metadata[ref.type + ':' + ref.id + ':name'];
      }
      return ref.id;
    };

    vm.getDisplayableProperties = (properties)=> {
      return _.map(properties, (v, k)=> vm.metadata[k+":"+v+":name"]);
    };

    vm.search = ()=> {
      vm.query.page = 1;
      if (vm.query.mgmt) {
        vm.query.api = null;
        vm.query.application = null;
      }

      AuditService.list(vm.query, vm.api).then((response)=> {
        vm.handleAuditResponseData(response.data);
      });
    };
  }
};

export default AuditComponent;
