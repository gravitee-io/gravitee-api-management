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
const ApplicationCreationStep4Component: ng.IComponentOptions = {
  require: {
    parent: '^createApplication'
  },
  template: require("./application-creation-step4.html"),
  controller: function(Constants) {
    'ngInject';
    if (Constants.documentation && Constants.documentation.url) {
      this.url = Constants.documentation.url;
    } else {
      this.url = 'https://docs.gravitee.io';
    }
  }
};

export default ApplicationCreationStep4Component;
