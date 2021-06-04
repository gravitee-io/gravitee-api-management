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
function DialogConfirmDeploymentController($scope, $mdDialog, locals) {
  'ngInject';

  this.title = locals.title;
  this.msg = locals.msg;
  this.more = locals.more;
  this.deploymentLabel = '';
  this.confirmButton = locals.confirmButton || 'OK';
  this.cancelButton = locals.cancelButton || 'Cancel';

  this.cancel = function () {
    $mdDialog.hide(false);
  };

  this.confirm = () => {
    $mdDialog.hide(this.deploymentLabel);
  };
}

export default DialogConfirmDeploymentController;
