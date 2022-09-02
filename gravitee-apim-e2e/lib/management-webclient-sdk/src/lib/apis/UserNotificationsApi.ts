/* tslint:disable */
/* eslint-disable */
/**
 * Gravitee.io - Management API
 * Some news resources are in alpha version. This implies that they are likely to be modified or even removed in future versions. They are marked with the 🧪 symbol
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  PortalNotificationPageResult,
} from '../models';
import {
    PortalNotificationPageResultFromJSON,
    PortalNotificationPageResultToJSON,
} from '../models';

export interface DeleteAllUserNotificationsRequest {
    orgId: string;
}

export interface DeleteAllUserNotifications1Request {
    envId: string;
    orgId: string;
}

export interface DeleteUserNotificationRequest {
    notification: string;
    orgId: string;
}

export interface DeleteUserNotification1Request {
    notification: string;
    envId: string;
    orgId: string;
}

export interface GetUserNotificationsRequest {
    orgId: string;
}

export interface GetUserNotifications1Request {
    envId: string;
    orgId: string;
}

/**
 * 
 */
export class UserNotificationsApi extends runtime.BaseAPI {

    /**
     * Delete all user\'s notifications
     */
    async deleteAllUserNotificationsRaw(requestParameters: DeleteAllUserNotificationsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling deleteAllUserNotifications.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/user/notifications`.replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete all user\'s notifications
     */
    async deleteAllUserNotifications(requestParameters: DeleteAllUserNotificationsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteAllUserNotificationsRaw(requestParameters, initOverrides);
    }

    /**
     * Delete all user\'s notifications
     */
    async deleteAllUserNotifications1Raw(requestParameters: DeleteAllUserNotifications1Request, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.envId === null || requestParameters.envId === undefined) {
            throw new runtime.RequiredError('envId','Required parameter requestParameters.envId was null or undefined when calling deleteAllUserNotifications1.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling deleteAllUserNotifications1.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/environments/{envId}/user/notifications`.replace(`{${"envId"}}`, encodeURIComponent(String(requestParameters.envId))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete all user\'s notifications
     */
    async deleteAllUserNotifications1(requestParameters: DeleteAllUserNotifications1Request, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteAllUserNotifications1Raw(requestParameters, initOverrides);
    }

    /**
     * Delete a single user\'s notification
     */
    async deleteUserNotificationRaw(requestParameters: DeleteUserNotificationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.notification === null || requestParameters.notification === undefined) {
            throw new runtime.RequiredError('notification','Required parameter requestParameters.notification was null or undefined when calling deleteUserNotification.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling deleteUserNotification.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/user/notifications/{notification}`.replace(`{${"notification"}}`, encodeURIComponent(String(requestParameters.notification))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete a single user\'s notification
     */
    async deleteUserNotification(requestParameters: DeleteUserNotificationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteUserNotificationRaw(requestParameters, initOverrides);
    }

    /**
     * Delete a single user\'s notification
     */
    async deleteUserNotification1Raw(requestParameters: DeleteUserNotification1Request, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.notification === null || requestParameters.notification === undefined) {
            throw new runtime.RequiredError('notification','Required parameter requestParameters.notification was null or undefined when calling deleteUserNotification1.');
        }

        if (requestParameters.envId === null || requestParameters.envId === undefined) {
            throw new runtime.RequiredError('envId','Required parameter requestParameters.envId was null or undefined when calling deleteUserNotification1.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling deleteUserNotification1.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/environments/{envId}/user/notifications/{notification}`.replace(`{${"notification"}}`, encodeURIComponent(String(requestParameters.notification))).replace(`{${"envId"}}`, encodeURIComponent(String(requestParameters.envId))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete a single user\'s notification
     */
    async deleteUserNotification1(requestParameters: DeleteUserNotification1Request, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteUserNotification1Raw(requestParameters, initOverrides);
    }

    /**
     * List user\'s notifications
     */
    async getUserNotificationsRaw(requestParameters: GetUserNotificationsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<PortalNotificationPageResult>> {
        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling getUserNotifications.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/user/notifications`.replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => PortalNotificationPageResultFromJSON(jsonValue));
    }

    /**
     * List user\'s notifications
     */
    async getUserNotifications(requestParameters: GetUserNotificationsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<PortalNotificationPageResult> {
        const response = await this.getUserNotificationsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * List user\'s notifications
     */
    async getUserNotifications1Raw(requestParameters: GetUserNotifications1Request, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<PortalNotificationPageResult>> {
        if (requestParameters.envId === null || requestParameters.envId === undefined) {
            throw new runtime.RequiredError('envId','Required parameter requestParameters.envId was null or undefined when calling getUserNotifications1.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling getUserNotifications1.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/environments/{envId}/user/notifications`.replace(`{${"envId"}}`, encodeURIComponent(String(requestParameters.envId))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => PortalNotificationPageResultFromJSON(jsonValue));
    }

    /**
     * List user\'s notifications
     */
    async getUserNotifications1(requestParameters: GetUserNotifications1Request, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<PortalNotificationPageResult> {
        const response = await this.getUserNotifications1Raw(requestParameters, initOverrides);
        return await response.value();
    }

}
