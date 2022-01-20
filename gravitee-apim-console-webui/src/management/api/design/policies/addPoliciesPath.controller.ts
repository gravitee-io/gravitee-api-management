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

class AddPoliciesPathController {
  private newPath: { path: string; copyFromRootPath: boolean };
  private paths: any;
  private rootCtrl: any;
  private canCopyFromRootPath: boolean;

  constructor(private $mdDialog: ng.material.IDialogService, private locals) {
    'ngInject';
    this.paths = locals.paths;
    this.rootCtrl = locals.rootCtrl;
    this.canCopyFromRootPath = this.paths['/'] !== undefined;
    this.newPath = {
      path: '',
      copyFromRootPath: this.canCopyFromRootPath,
    };
  }

  hide() {
    this.$mdDialog.cancel();
  }

  add() {
    if (this.newPath.copyFromRootPath) {
      this.paths[this.newPath.path] = _.cloneDeep(this.paths['/']);
      _.forEach(this.paths[this.newPath.path], (policy) => {
        // eslint-disable-next-line angular/no-private-call
        delete policy.$$hashKey;
      });
    } else {
      this.paths[this.newPath.path] = [];
    }
    this.$mdDialog.hide(this.paths);
  }
}

export default AddPoliciesPathController;
