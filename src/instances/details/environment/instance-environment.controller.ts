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

class InstanceEnvironmentController {
  private instance: any;
  private systemPropertiesProvider: any;
  private filterSystemProperty: string;

  constructor() {
    'ngInject';
  }

  $onInit() {
    this.systemPropertiesProvider = this.sort(this.instance.systemProperties);
    this.instance.systemProperties = this.systemPropertiesProvider;
  }

  sort(systemProperties) {
    let systemPropertiesProvider = {};
    _.forEach(_.sortBy(_.keys(systemProperties)), function (key) {
      systemPropertiesProvider[key] = systemProperties[key];
    });
    return systemPropertiesProvider;
  }

  filter() {
    this.instance.systemProperties = {};
    let lowerFilter = this.filterSystemProperty.toLowerCase();
    _.forEach(this.systemPropertiesProvider, (value, key) => {
      if (_.includes(key.toLowerCase(), lowerFilter) || _.includes(value.toLowerCase(), lowerFilter)) {
        this.instance.systemProperties[key] = value;
      }
    });
  }
}

export default InstanceEnvironmentController;
