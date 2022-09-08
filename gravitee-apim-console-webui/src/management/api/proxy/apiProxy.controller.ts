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

import SidenavService from '../../../components/sidenav/sidenav.service';
import { ApiService } from '../../../services/api.service';
import CorsService from '../../../services/cors.service';
import GroupService from '../../../services/group.service';
import NotificationService from '../../../services/notification.service';
import UserService from '../../../services/user.service';

class ApiProxyController {
  private initialApi: any;
  private initialDiscovery: any;

  private api: any;
  private groups: any;
  private categories: any;
  private tags: any;
  private tenants: any;
  private failoverEnabled: boolean;
  private contextPathEditable: boolean;
  private formApi: any;
  private apiPublic: boolean;
  private headers: string[];
  private discovery: any;
  private virtualHostModeEnabled: boolean;
  private domainRestrictions: string[];
  private domainRegexList: RegExp[] = [];
  private hostPattern: string;
  // RFC 6454 section-7.1, serialized-origin regex from RFC 3986
  private allowOriginPattern = '^((\\*)|(null)|(^(([^:\\/?#]+):)?(\\/\\/([^\\/?#]*))?))$';

  constructor(
    private ApiService: ApiService,
    private CorsService: CorsService,
    private NotificationService: NotificationService,
    private UserService: UserService,
    private $scope,
    private $mdDialog,
    private $mdEditDialog,
    private $rootScope,
    private $state,
    private GroupService: GroupService,
    private SidenavService: SidenavService,
    private resolvedCategories,
    private resolvedCurrentEnvironment,
    private resolvedGroups,
    private resolvedTags,
    private resolvedTenants,
    private userTags,
  ) {
    'ngInject';

    this.ApiService = ApiService;
    this.CorsService = CorsService;
    this.NotificationService = NotificationService;
    this.UserService = UserService;
    this.GroupService = GroupService;
    this.$scope = $scope;
    this.$rootScope = $rootScope;
    this.$mdEditDialog = $mdEditDialog;
    this.$mdDialog = $mdDialog;
    this.initialApi = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.discovery = this.api.services && this.api.services.discovery;
    this.discovery = this.discovery || { enabled: false, configuration: {} };
    this.initialDiscovery = _.cloneDeep(this.discovery);
    this.tenants = resolvedTenants.data;
    this.$scope.selected = [];
    if (resolvedCurrentEnvironment) {
      this.initDomainRestrictions(resolvedCurrentEnvironment.data);
    }
    this.$scope.searchHeaders = null;

    this.api.labels = this.api.labels || [];

    this.virtualHostModeEnabled =
      this.api.proxy.virtual_hosts.length > 1 || this.api.proxy.virtual_hosts[0].host !== undefined || this.domainRestrictions.length > 0;

    this.$scope.lbs = [
      {
        name: 'Round-Robin',
        value: 'ROUND_ROBIN',
      },
      {
        name: 'Random',
        value: 'RANDOM',
      },
      {
        name: 'Weighted Round-Robin',
        value: 'WEIGHTED_ROUND_ROBIN',
      },
      {
        name: 'Weighted Random',
        value: 'WEIGHTED_RANDOM',
      },
    ];

    this.$scope.methods = CorsService.getHttpMethods();

    this.initState();

    this.categories = resolvedCategories;

    this.tags = resolvedTags;
    this.groups = resolvedGroups;

    this.headers = ApiService.defaultHttpHeaders();

    this.$scope.$on('apiChangeSuccess', (event, args) => {
      this.api = args.api;
    });
  }

  toggleVisibility() {
    if (this.api.visibility === 'PUBLIC') {
      this.api.visibility = 'PRIVATE';
    } else {
      this.api.visibility = 'PUBLIC';
    }
    this.formApi.$setDirty();
  }

  initState() {
    this.$scope.apiEnabled = this.$scope.$parent.apiCtrl.api.state === 'STARTED';
    this.$scope.apiPublic = this.$scope.$parent.apiCtrl.api.visibility === 'PUBLIC';

    // Failover
    this.failoverEnabled = this.api.proxy.failover !== undefined;

    // Context-path editable
    this.contextPathEditable = this.UserService.currentUser.id === this.api.owner.id;

    this.api.proxy.cors =
      this.api.proxy.cors && this.api.proxy.cors.allowOrigin
        ? this.api.proxy.cors
        : {
            allowOrigin: [],
            allowHeaders: [],
            allowMethods: [],
            exposeHeaders: [],
            maxAge: -1,
            allowCredentials: false,
          };
  }

  removeEndpoints() {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete endpoint(s)?',
          msg: '',
          confirmButton: 'Delete',
        },
      })
      .then((response) => {
        if (response) {
          _(this.$scope.selected).forEach((endpoint) => {
            _(this.api.proxy.groups).forEach((group) => {
              _(group.endpoints).forEach((endpoint2, index, object) => {
                if (endpoint2 !== undefined && endpoint2.name === endpoint.name) {
                  (object as any[]).splice(index, 1);
                }
              });
            });
          });

          this.update(this.api);
        }
      });
  }

  reset() {
    this.api = _.cloneDeep(this.initialApi);
    this.discovery = _.cloneDeep(this.initialDiscovery);

    this.initState();

    if (this.formApi) {
      this.formApi.$setPristine();
      this.formApi.$setUntouched();
    }
  }

  delete(id) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: "Are you sure you want to delete '" + this.api.name + "' API?",
          msg: '',
          confirmButton: 'Delete',
        },
      })
      .then((response) => {
        if (response) {
          this.ApiService.delete(id).then(() => {
            this.NotificationService.show("API '" + this.initialApi.name + "' has been removed");
            this.$state.go('management.apis.list', {}, { reload: true });
          });
        }
      });
  }

  onApiUpdate(updatedApi) {
    this.api = updatedApi;
    this.initialApi = _.cloneDeep(updatedApi);
    this.initState();
    this.formApi.$setPristine();
    this.$rootScope.$broadcast('apiChangeSuccess', { api: _.cloneDeep(updatedApi) });
    this.NotificationService.show("API '" + this.initialApi.name + "' saved");
    this.SidenavService.setCurrentResource(this.api.name);
  }

  update(api) {
    if (!this.failoverEnabled) {
      delete api.proxy.failover;
    }
    this.ApiService.update(api).then((updatedApi) => {
      updatedApi.data.etag = updatedApi.headers('etag');
      this.onApiUpdate(updatedApi.data);
    });
  }

  getTenants(tenants) {
    if (tenants !== undefined) {
      return _(tenants)
        .map((tenant) => _.find(this.tenants, { id: tenant }))
        .map((tenant: any) => tenant.name)
        .join(', ');
    }

    return '';
  }

  hasTenants(): boolean {
    return this.tenants && this.tenants.length;
  }

  getGroup(groupId) {
    return _.find(this.groups, { id: groupId });
  }

  /**
   * Search for HTTP Headers.
   */
  querySearchHeaders(query) {
    return this.CorsService.querySearchHeaders(query, this.headers);
  }

  createGroup() {
    this.$state.go('management.apis.detail.proxy.group', { groupName: '' });
  }

  deleteGroup(groupname) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete group ' + groupname + '?',
          msg: '',
          confirmButton: 'Delete group',
        },
      })
      .then((response) => {
        if (response) {
          _(this.api.proxy.groups).forEach((group, index, object) => {
            if (group.name !== undefined && group.name === groupname) {
              (object as any[]).splice(index, 1);
              this.update(this.api);
            }
          });
        }
      });
  }

  hasHealthCheck(endpoint: any) {
    // FIXME: https://github.com/gravitee-io/issues/issues/6437
    if (endpoint.backup || (endpoint.type.toLowerCase() !== 'http' && endpoint.type.toLowerCase() !== 'grpc')) {
      return false;
    }

    if (endpoint.healthcheck !== undefined) {
      return endpoint.healthcheck.enabled;
    } else {
      return this.api.services && this.api.services['health-check'] && this.api.services['health-check'].enabled;
    }
  }

  isTagDisabled(tag: any): boolean {
    return !_.includes(this.userTags, tag.id);
  }

  controlAllowOrigin(chip, index) {
    this.CorsService.controlAllowOrigin(chip, index, this.api.proxy.cors.allowOrigin);
  }

  isRegexValid() {
    return this.CorsService.isRegexValid(this.api.proxy.cors.allowOrigin);
  }

  switchVirtualHostMode() {
    if (this.virtualHostModeEnabled) {
      this.$mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Switch to context-path mode',
            msg: 'By moving back to context-path you will loose all virtual-hosts. Are you sure to continue?',
            confirmButton: 'Switch',
          },
        })
        .then((response) => {
          if (response) {
            // Keep only the first virtual_host and remove the host
            this.api.proxy.virtual_hosts.splice(1);
            this.api.proxy.virtual_hosts[0].host = undefined;

            this.virtualHostModeEnabled = !this.virtualHostModeEnabled;

            this.update(this.api);
          }
        });
    } else if (this.formApi.$dirty) {
      this.virtualHostModeEnabled = !this.virtualHostModeEnabled;
      this.update(this.api);
    } else {
      this.virtualHostModeEnabled = !this.virtualHostModeEnabled;
    }
  }

  addVirtualHost() {
    if (this.api.proxy.virtual_hosts === undefined) {
      this.api.proxy.virtual_hosts = [];
    }

    this.api.proxy.virtual_hosts.push({ host: undefined, path: undefined });
  }

  removeVirtualHost(idx) {
    if (this.api.proxy.virtual_hosts !== undefined) {
      this.api.proxy.virtual_hosts.splice(idx, 1);
      this.formApi.$setDirty();
    }
  }

  getHostOptions(host: string): string[] {
    let myHost = host;
    if (myHost) {
      this.domainRegexList.forEach((regex) => (myHost = myHost.replace(regex, '')));
    }

    if (myHost && myHost !== '' && !_.includes(this.domainRestrictions, myHost)) {
      return this.domainRestrictions.map((domain) => myHost + '.' + domain);
    }

    return this.domainRestrictions;
  }

  initDomainRestrictions(data: any) {
    const domainPattern = '((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+';

    this.domainRestrictions = data.domainRestrictions;

    if (this.domainRestrictions === undefined) {
      this.domainRestrictions = [];
    }

    if (this.domainRestrictions.length === 0) {
      this.hostPattern = '^' + domainPattern + '[A-Za-z]{2,6}$';
    } else {
      this.hostPattern = this.domainRestrictions.map((value) => '^' + domainPattern + value + '$').join('|');
    }

    // Prepare host regex (used to assist user when specifying an host).
    this.domainRegexList = this.domainRestrictions.map((value) => new RegExp('\\.?' + value, 'i'));
  }

  onFocus(inputId: string) {
    const input: HTMLInputElement = document.querySelector('#' + inputId);
    if (input.value) {
      for (const domainRegex of this.domainRegexList) {
        const match = input.value.match(domainRegex);

        if (match) {
          const index = input.value.indexOf(match[0]);
          if (input.selectionStart > index) {
            input.setSelectionRange(index, index);
          }
          break;
        }
      }
    }
  }

  onBlur(vHost: any, inputId: string) {
    const input: HTMLInputElement = document.querySelector('#' + inputId);
    if (input.value) {
      if (input.value.startsWith('.')) {
        input.value = input.value.replace('.', '');
      }
    }
    if (vHost.host) {
      if (vHost.host.startsWith('.')) {
        vHost.host = vHost.host.replace('.', '');
      }
    }
    vHost.host = input.value;
  }
}

export default ApiProxyController;
