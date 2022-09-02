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

import { exists, mapValues } from '../runtime';
import type { ApiKeyMode } from './ApiKeyMode';
import {
    ApiKeyModeFromJSON,
    ApiKeyModeFromJSONTyped,
    ApiKeyModeToJSON,
} from './ApiKeyMode';
import type { ApplicationSettings } from './ApplicationSettings';
import {
    ApplicationSettingsFromJSON,
    ApplicationSettingsFromJSONTyped,
    ApplicationSettingsToJSON,
} from './ApplicationSettings';

/**
 * 
 * @export
 * @interface UpdateApplicationEntity
 */
export interface UpdateApplicationEntity {
    /**
     * 
     * @type {ApiKeyMode}
     * @memberof UpdateApplicationEntity
     */
    api_key_mode?: ApiKeyMode;
    /**
     * 
     * @type {string}
     * @memberof UpdateApplicationEntity
     */
    background?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateApplicationEntity
     */
    clientId?: string;
    /**
     * Application's description. A short description of your App.
     * @type {string}
     * @memberof UpdateApplicationEntity
     */
    description: string;
    /**
     * 
     * @type {boolean}
     * @memberof UpdateApplicationEntity
     */
    disable_membership_notifications?: boolean;
    /**
     * Domain used by the application, if relevant
     * @type {string}
     * @memberof UpdateApplicationEntity
     */
    domain?: string;
    /**
     * Application groups. Used to add teams to your application.
     * @type {Array<string>}
     * @memberof UpdateApplicationEntity
     */
    groups?: Array<string>;
    /**
     * Application's name. Duplicate names can exists.
     * @type {string}
     * @memberof UpdateApplicationEntity
     */
    name: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateApplicationEntity
     */
    picture?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateApplicationEntity
     */
    picture_url?: string;
    /**
     * 
     * @type {ApplicationSettings}
     * @memberof UpdateApplicationEntity
     */
    settings: ApplicationSettings;
    /**
     * a string to describe the type of your app.
     * @type {string}
     * @memberof UpdateApplicationEntity
     */
    type?: string;
}

/**
 * Check if a given object implements the UpdateApplicationEntity interface.
 */
export function instanceOfUpdateApplicationEntity(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "description" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "settings" in value;

    return isInstance;
}

export function UpdateApplicationEntityFromJSON(json: any): UpdateApplicationEntity {
    return UpdateApplicationEntityFromJSONTyped(json, false);
}

export function UpdateApplicationEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): UpdateApplicationEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'api_key_mode': !exists(json, 'api_key_mode') ? undefined : ApiKeyModeFromJSON(json['api_key_mode']),
        'background': !exists(json, 'background') ? undefined : json['background'],
        'clientId': !exists(json, 'clientId') ? undefined : json['clientId'],
        'description': json['description'],
        'disable_membership_notifications': !exists(json, 'disable_membership_notifications') ? undefined : json['disable_membership_notifications'],
        'domain': !exists(json, 'domain') ? undefined : json['domain'],
        'groups': !exists(json, 'groups') ? undefined : json['groups'],
        'name': json['name'],
        'picture': !exists(json, 'picture') ? undefined : json['picture'],
        'picture_url': !exists(json, 'picture_url') ? undefined : json['picture_url'],
        'settings': ApplicationSettingsFromJSON(json['settings']),
        'type': !exists(json, 'type') ? undefined : json['type'],
    };
}

export function UpdateApplicationEntityToJSON(value?: UpdateApplicationEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'api_key_mode': ApiKeyModeToJSON(value.api_key_mode),
        'background': value.background,
        'clientId': value.clientId,
        'description': value.description,
        'disable_membership_notifications': value.disable_membership_notifications,
        'domain': value.domain,
        'groups': value.groups,
        'name': value.name,
        'picture': value.picture,
        'picture_url': value.picture_url,
        'settings': ApplicationSettingsToJSON(value.settings),
        'type': value.type,
    };
}

