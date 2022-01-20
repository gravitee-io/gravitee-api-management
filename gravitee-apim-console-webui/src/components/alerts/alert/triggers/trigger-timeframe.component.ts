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
import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-date-picker';
import '@gravitee/ui-components/wc/gv-row-expandable';
import { deepEqual } from '@gravitee/ui-components/src/lib/utils';
import moment from 'moment';

import { Period } from '../../../../entities/alert';

const AlertTriggerTimeframeComponent: ng.IComponentOptions = {
  bindings: {
    alert: '<',
    form: '<',
  },
  template: require('./trigger-timeframe.html'),
  controller: [
    '$scope',
    function ($scope) {
      this.officeStartTime = moment();
      this.officeStartTime.set({ hour: 9, minute: 0, second: 0, millisecond: 0 });
      this.officeEndTime = moment();
      this.officeEndTime.set({ hour: 18, minute: 0, second: 0, millisecond: 0 });

      $scope.open = false;
      this.$onInit = () => {
        this.days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
        const businessDays = [...this.days.slice(0, 5)];
        const officeHours = [this.officeStartTime.valueOf(), this.officeEndTime.valueOf()];
        this.alert.notificationPeriods = this.alert.notificationPeriods || [];
        $scope.timeframe = { days: [], range: [], officeHours: false, businessDays: false };
        $scope.$watchCollection('timeframe', (newTimeframe, oldTimeframe) => {
          if (newTimeframe.businessDays !== oldTimeframe.businessDays) {
            if (newTimeframe.businessDays) {
              $scope.timeframe.days = [...this.days.slice(0, 5)];
            } else {
              $scope.timeframe.days = [];
            }
          } else {
            $scope.timeframe.businessDays = deepEqual($scope.timeframe.days, businessDays, true);
          }

          if (newTimeframe.officeHours !== oldTimeframe.officeHours) {
            if (newTimeframe.officeHours) {
              $scope.timeframe.range = officeHours;
            } else {
              $scope.timeframe.range = [];
            }
          } else {
            $scope.timeframe.officeHours = deepEqual($scope.timeframe.range, officeHours, true);
          }
        });
      };

      this.hasTime = (period) => {
        return period.beginHour != null && period.endHour != null;
      };

      this.isRange = (period) => {
        return this.hasTime(period) && period.beginHour !== period.endHour;
      };

      this.isTime = (period) => {
        return this.hasTime(period) && period.beginHour === period.endHour;
      };

      this.add = (form) => {
        form.$setSubmitted();
        if (form.$valid) {
          const { days, range } = form;

          let beginHour = null;
          let endHour = null;
          if (range != null && range.length === 2) {
            const midnight = moment().startOf('day');
            beginHour = moment(range[0]).diff(midnight, 'seconds');
            endHour = moment(range[1]).diff(midnight, 'seconds');
          }

          const period = new Period({
            days: days.map((day) => this.days.indexOf(day) + 1).sort(),
            beginHour,
            endHour,
            zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
          });
          const periods: Array<Period> = this.alert.notificationPeriods;
          const alreadyAdd = periods.find((p) => period.equals(p)) != null;
          if (!alreadyAdd) {
            this.alert.notificationPeriods.push(period);
          }
          form.days = [];
          form.range = [];
          form.officeHours = false;
          form.businessDays = false;
          form.$setDirty();
          form.$setPristine();
          $scope.open = false;
        }
      };

      this.formatPeriod = (secondsSinceStartOfDay, format = 'LTS') => {
        const midnight = moment().startOf('day');
        midnight.add(secondsSinceStartOfDay, 'seconds');
        return midnight.format(format);
      };

      this.formatTime = (date, format = 'LTS') => {
        return moment(date).format(format);
      };

      this.getOfficeHoursDescription = () => {
        return `Set time range from ${this.formatTime(this.officeStartTime, 'LT')} to ${this.formatTime(this.officeEndTime, 'LT')}`;
      };

      this.remove = (idx: number) => {
        const period = this.alert.notificationPeriods.splice(idx, 1);
        this.form.$setDirty();
        return period[0];
      };

      this.edit = (idx: number) => {
        $scope.open = true;
        const period = this.remove(idx);
        $scope.timeframe.days = this.getDays(period.days);

        if (period.beginHour != null && period.endHour != null) {
          const midnight = moment().startOf('day');
          $scope.timeframe.range = [
            midnight.clone().add(period.beginHour, 'seconds').valueOf(),
            midnight.clone().add(period.endHour, 'seconds').valueOf(),
          ];
        }
      };

      this.onToggleForm = ({ detail }) => {
        $scope.open = detail.open;
      };

      this.getDays = (daysOfWeek: Array<number>) => {
        return daysOfWeek.sort().map((dayOfWeek) => this.days[dayOfWeek - 1]);
      };

      this.getDayNames = (period: Period) => {
        if (period.days) {
          if (period.days.length === this.days.length) {
            return 'day';
          } else if (period.days.length === 1) {
            return this.days[period.days[0] - 1];
          }
          const days = this.getDays(period.days);
          const lastDay = days.pop();
          return `${days.join(', ')} and ${lastDay}`;
        }
        return '';
      };
    },
  ],
};

export default AlertTriggerTimeframeComponent;
