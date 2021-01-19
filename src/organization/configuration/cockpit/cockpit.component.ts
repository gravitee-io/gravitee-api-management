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
import InstallationService from '../../../services/installation.service';

const CockpitComponent: ng.IComponentOptions = {
  bindings: {
  },
  template: require('./cockpit.html'),
  controller: function(
    InstallationService: InstallationService,
  ) {
    'ngInject';
    this.$onInit = () => {
      InstallationService.getInstallationInformation().then( response => {
        const installation = response.data;
        const cockpitLink = `<a href="${installation.cockpitURL}" target="_blank">Cockpit</a>`;
        if (!installation.additionalInformation.COCKPIT_INSTALLATION_STATUS) {
          // DEFAULT
          this.icon = 'explore';
          this.message = 'Meet Cockpit...';
          this.subMessage = `Create an account on ${cockpitLink}, register your current installation and start creating new organizations and environments!`;

        } else if (installation.additionalInformation.COCKPIT_INSTALLATION_STATUS === 'PENDING') {
          // PENDING
          this.icon = 'schedule';
          this.message = 'Almost there!';
          this.subMessage = `Your installation is connected but it still has to be accepted on ${cockpitLink}!`;

        } else if (installation.additionalInformation.COCKPIT_INSTALLATION_STATUS === 'ACCEPTED') {
          // ACCEPTED
          this.icon = 'check_circle';
          this.message = 'Congratulation!';
          this.subMessage = `Your installation is now connected to ${cockpitLink}, you can now explore all the possibilities offered by Cockpit!`;

        } else if (installation.additionalInformation.COCKPIT_INSTALLATION_STATUS === 'REJECTED') {
          // REJECTED
          this.icon = 'warning';
          this.message = 'No luck!';
          this.subMessage = `Seems that your installation is connected to ${cockpitLink}, but has been rejected...`;
        }

        document.querySelector('.gv-cockpit_submessage').innerHTML = this.subMessage;
      });
    };
  }
};

export default CockpitComponent;
