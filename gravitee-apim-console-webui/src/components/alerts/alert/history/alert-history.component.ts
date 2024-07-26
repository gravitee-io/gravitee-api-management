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

import AlertService from '../../../../services/alert.service';

const AlertHistoryComponent: ng.IComponentOptions = {
  bindings: {
    alert: '<',
  },
  require: {
    parent: '^alertComponentAjs',
  },
  template: require('html-loader!./alert-history.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: [
    'AlertService',
    function (AlertService: AlertService) {
      this.query = {
        limit: 10,
        page: 1,
      };

      this.$onInit = () => {
        AlertService.listAlertEvents(this.alert).then((response) => {
          this.events = response.data;
        });
      };

      this.search = () => {
        AlertService.listAlertEvents(this.alert, {
          from: this.lastFrom,
          to: this.lastTo,
          page: this.query.page - 1,
          size: this.query.limit,
        }).then((response) => {
          this.events = response.data;
        });
      };
    },
  ],
};

export default AlertHistoryComponent;
