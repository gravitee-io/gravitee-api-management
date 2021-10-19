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

import { ApiService } from '../../../../../services/api.service';

function DialogApiDuplicateController($mdDialog, ApiService: ApiService, api) {
  'ngInject';
  this.contextPathPlaceholder = api.proxy.virtual_hosts[0].path;
  this.versionPlaceholder = api.version;
  this.filteredFields = [
    { id: 'groups', description: 'Groups', checked: true },
    { id: 'members', description: 'Members', checked: true },
    { id: 'pages', description: 'Pages', checked: true },
    { id: 'plans', description: 'Plans', checked: true },
  ];

  this.hide = () => {
    $mdDialog.hide();
  };

  this.duplicate = () => {
    const config = {
      context_path: this.contextPath,
      version: this.version,
      filtered_fields: _.map(
        _.filter(this.filteredFields, (fl: any) => {
          return !fl.checked;
        }),
        'id',
      ),
    };
    ApiService.duplicate(api.id, config).then((response) => {
      $mdDialog.hide(response.data);
    });
  };

  this.onContextPathChanged = () => {
    this.contextPathInvalid = false;
    ApiService.verify({ context_path: this.contextPath }, { ignoreLoadingBar: true, silentCall: true }).then(
      () => {
        this.contextPathInvalid = false;
      },
      () => {
        this.contextPathInvalid = true;
      },
    );
  };
}

export default DialogApiDuplicateController;
