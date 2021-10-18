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
import { DampeningMode, DurationTimeUnit } from '../../../../entities/alert';

const AlertTriggerDampeningComponent: ng.IComponentOptions = {
  bindings: {
    dampening: '<',
  },
  require: {
    parent: '^alertComponent',
  },
  template: require('./trigger-dampening.html'),
  controller: function () {
    'ngInject';

    this.$onInit = () => {
      this.modes = DampeningMode.MODES;
      this.timeUnits = DurationTimeUnit.TIME_UNITS;
    };

    this.onModeChange = () => {
      delete this.dampening.duration;
      delete this.dampening.timeUnit;
      delete this.dampening.trueEvaluations;
      delete this.dampening.totalEvaluations;
    };
  },
};

export default AlertTriggerDampeningComponent;
