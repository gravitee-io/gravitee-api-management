"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
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
var _ = require("lodash");
var AddPoliciesPathController = (function () {
    function AddPoliciesPathController($mdDialog, locals) {
        'ngInject';
        this.$mdDialog = $mdDialog;
        this.locals = locals;
        this.paths = locals.paths;
        this.rootCtrl = locals.rootCtrl;
        this.newPath = {
            path: '',
            copyFromRootPath: true
        };
    }
    AddPoliciesPathController.prototype.hide = function () {
        this.$mdDialog.cancel();
    };
    AddPoliciesPathController.prototype.add = function () {
        if (this.newPath.copyFromRootPath) {
            this.paths[this.newPath.path] = _.cloneDeep(this.paths["/"]);
            _.forEach(this.paths[this.newPath.path], function (policy) {
                delete policy.$$hashKey;
            });
        }
        else {
            this.paths[this.newPath.path] = [];
        }
        this.$mdDialog.hide(this.paths);
    };
    return AddPoliciesPathController;
}());
exports.default = AddPoliciesPathController;
