"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var ApiAdminController = (function () {
    function ApiAdminController(resolvedApi, $state, $scope, $rootScope, $mdDialog, ApiService, NotificationService, resolvedApiState, SidenavService) {
        'ngInject';
        this.resolvedApi = resolvedApi;
        this.$state = $state;
        this.$scope = $scope;
        this.$rootScope = $rootScope;
        this.$mdDialog = $mdDialog;
        this.ApiService = ApiService;
        this.NotificationService = NotificationService;
        this.resolvedApiState = resolvedApiState;
        this.SidenavService = SidenavService;
        this.$scope = $scope;
        this.$state = $state;
        this.$mdDialog = $mdDialog;
        this.$rootScope = $rootScope;
        this.api = resolvedApi.data;
        SidenavService.setCurrentResource(this.api.name);
        this.ApiService = ApiService;
        this.NotificationService = NotificationService;
        this.apiJustDeployed = false;
        this.apiIsSynchronized = resolvedApiState.data.is_synchronized;
        this.init();
    }
    ApiAdminController.prototype.init = function () {
        var self = this;
        this.$scope.$on("apiPictureChangeSuccess", function (event, args) {
            self.api.picture = args.image;
            self.updatePicture(self.api);
        });
        this.$scope.$on("apiChangeSuccess", function () {
            self.checkAPISynchronization(self.api);
            self.$rootScope.$broadcast("apiChangeSucceed");
        });
    };
    ApiAdminController.prototype.checkAPISynchronization = function (api) {
        var _this = this;
        this.ApiService.isAPISynchronized(api.id).then(function (response) {
            _this.apiJustDeployed = false;
            if (response.data.is_synchronized) {
                _this.apiIsSynchronized = true;
            }
            else {
                _this.apiIsSynchronized = false;
            }
            _this.$rootScope.$broadcast("checkAPISynchronizationSucceed");
        });
    };
    ApiAdminController.prototype.showDeployAPIConfirm = function (ev, api) {
        ev.stopPropagation();
        var self = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../components/dialog/confirm.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Would you like to deploy your API ?',
                confirmButton: 'OK'
            }
        }).then(function (response) {
            if (response) {
                self.deploy(api);
            }
        });
    };
    ApiAdminController.prototype.deploy = function (api) {
        var _this = this;
        this.ApiService.deploy(api.id).then(function (deployedApi) {
            _this.NotificationService.show("API deployed");
            _this.api = deployedApi.data;
            _this.api.picture_url = api.picture_url;
            _this.apiJustDeployed = true;
            _this.$rootScope.$broadcast("apiChangeSuccess");
        });
    };
    ApiAdminController.prototype.updatePicture = function (api) {
        var self = this;
        this.ApiService.update(api).then(function (updatedApi) {
            self.api = updatedApi.data;
            self.NotificationService.show('API \'' + self.api.name + '\' saved');
        });
    };
    ApiAdminController.prototype.isOwner = function () {
        return this.api.permission && (this.api.permission === 'owner' || this.api.permission === 'primary_owner');
    };
    return ApiAdminController;
}());
exports.default = ApiAdminController;
