"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var UserController = (function () {
    function UserController(UserService, NotificationService, $state, $scope) {
        'ngInject';
        this.UserService = UserService;
        this.NotificationService = NotificationService;
        this.$state = $state;
        this.$scope = $scope;
    }
    UserController.prototype.$onInit = function () {
        if (!this.user || (this.user && this.user.username === undefined)) {
            this.$state.go('login', {}, { reload: true, inherit: false });
        }
    };
    UserController.prototype.save = function () {
        var _this = this;
        this.UserService.save(this.user).then(function () {
            _this.$scope.formUser.$setPristine();
            _this.originalPicture = _this.user.picture;
            _this.NotificationService.show("User has been updated successfully");
        });
    };
    UserController.prototype.cancel = function () {
        this.user.picture = this.originalPicture;
        delete this.originalPicture;
    };
    return UserController;
}());
exports.default = UserController;
