"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var User = (function () {
    function User(username, password, authorities, accountNonExpired, accountNonLocked, credentialsNonExpired, enabled, picture) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
        this.picture = picture;
    }
    User.prototype.allowedTo = function (roles) {
        if (!roles || !this.authorities) {
            return false;
        }
        return this.authorities
            .some(function (authority) { return roles.indexOf(authority.authority) !== -1; });
    };
    return User;
}());
exports.User = User;
