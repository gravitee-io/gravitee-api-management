
import _ = require('lodash');

class ApiEndpointGroupController {
  private api: any;
  private group: any;
  private initialGroups: any;
  private initialGroup: any;

  private creation: boolean = false;

  constructor (
    private ApiService,
    private NotificationService,
    private $scope,
    private $rootScope,
    private $state,
    private $stateParams
  ) {
    'ngInject';

    this.api = this.$scope.$parent.apiCtrl.api;
    this.group = _.find(this.api.proxy.groups, { 'name': $stateParams.groupName});

    this.initialGroups = _.cloneDeep(this.api.proxy.groups);

    // Creation mode
    if (!this.group) {
      this.group = {};
      this.creation = true;
    }

    // Keep the initial state in case of form reset
    this.initialGroup = _.cloneDeep(this.group);

    this.$scope.lbs = [
      {
        name: 'Round-Robin',
        value: 'ROUND_ROBIN'
      }, {
        name: 'Random',
        value: 'RANDOM'
      }, {
        name: 'Weighted Round-Robin',
        value: 'WEIGHTED_ROUND_ROBIN'
      }, {
        name: 'Weighted Random',
        value: 'WEIGHTED_RANDOM'
      }];
  }

  update(api) {
    if (!_.includes(api.proxy.groups, this.group)) {
      api.proxy.groups.push(this.group);
    }

    this.ApiService.update(api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.api.etag = updatedApi.headers('etag');
      this.onApiUpdate();
      this.initialGroups = _.cloneDeep(api.proxy.groups);
    });
  }

  onApiUpdate() {
    this.$rootScope.$broadcast('apiChangeSuccess', {api: this.api});
    this.NotificationService.show('Endpoint saved');
    this.$state.go('management.apis.detail.proxy.endpoints');
  }

  reset() {
    this.$scope.formGroup.$setPristine();
    this.group = _.cloneDeep(this.initialGroup);
  }

  backToEndpointsConfiguration() {
    this.api.proxy.groups = _.cloneDeep(this.initialGroups);
    this.$state.go('management.apis.detail.proxy.endpoints');
  }
}

export default ApiEndpointGroupController;
