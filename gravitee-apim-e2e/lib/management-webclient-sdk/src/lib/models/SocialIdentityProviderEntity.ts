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
import type { IdentityProviderType } from './IdentityProviderType';
import {
    IdentityProviderTypeFromJSON,
    IdentityProviderTypeFromJSONTyped,
    IdentityProviderTypeToJSON,
} from './IdentityProviderType';

/**
 * 
 * @export
 * @interface SocialIdentityProviderEntity
 */
export interface SocialIdentityProviderEntity {
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    authorizationEndpoint?: string;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    clientId?: string;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    color?: string;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    description?: string;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    display?: string;
    /**
     * 
     * @type {boolean}
     * @memberof SocialIdentityProviderEntity
     */
    emailRequired?: boolean;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    id?: string;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    name?: string;
    /**
     * 
     * @type {Array<string>}
     * @memberof SocialIdentityProviderEntity
     */
    optionalUrlParams?: Array<string>;
    /**
     * 
     * @type {Array<string>}
     * @memberof SocialIdentityProviderEntity
     */
    requiredUrlParams?: Array<string>;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    scopeDelimiter?: string;
    /**
     * 
     * @type {Array<string>}
     * @memberof SocialIdentityProviderEntity
     */
    scopes?: Array<string>;
    /**
     * 
     * @type {boolean}
     * @memberof SocialIdentityProviderEntity
     */
    syncMappings?: boolean;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    tokenIntrospectionEndpoint?: string;
    /**
     * 
     * @type {IdentityProviderType}
     * @memberof SocialIdentityProviderEntity
     */
    type?: IdentityProviderType;
    /**
     * 
     * @type {string}
     * @memberof SocialIdentityProviderEntity
     */
    userLogoutEndpoint?: string;
}

/**
 * Check if a given object implements the SocialIdentityProviderEntity interface.
 */
export function instanceOfSocialIdentityProviderEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function SocialIdentityProviderEntityFromJSON(json: any): SocialIdentityProviderEntity {
    return SocialIdentityProviderEntityFromJSONTyped(json, false);
}

export function SocialIdentityProviderEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): SocialIdentityProviderEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'authorizationEndpoint': !exists(json, 'authorizationEndpoint') ? undefined : json['authorizationEndpoint'],
        'clientId': !exists(json, 'clientId') ? undefined : json['clientId'],
        'color': !exists(json, 'color') ? undefined : json['color'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'display': !exists(json, 'display') ? undefined : json['display'],
        'emailRequired': !exists(json, 'emailRequired') ? undefined : json['emailRequired'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'optionalUrlParams': !exists(json, 'optionalUrlParams') ? undefined : json['optionalUrlParams'],
        'requiredUrlParams': !exists(json, 'requiredUrlParams') ? undefined : json['requiredUrlParams'],
        'scopeDelimiter': !exists(json, 'scopeDelimiter') ? undefined : json['scopeDelimiter'],
        'scopes': !exists(json, 'scopes') ? undefined : json['scopes'],
        'syncMappings': !exists(json, 'syncMappings') ? undefined : json['syncMappings'],
        'tokenIntrospectionEndpoint': !exists(json, 'tokenIntrospectionEndpoint') ? undefined : json['tokenIntrospectionEndpoint'],
        'type': !exists(json, 'type') ? undefined : IdentityProviderTypeFromJSON(json['type']),
        'userLogoutEndpoint': !exists(json, 'userLogoutEndpoint') ? undefined : json['userLogoutEndpoint'],
    };
}

export function SocialIdentityProviderEntityToJSON(value?: SocialIdentityProviderEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'authorizationEndpoint': value.authorizationEndpoint,
        'clientId': value.clientId,
        'color': value.color,
        'description': value.description,
        'display': value.display,
        'emailRequired': value.emailRequired,
        'id': value.id,
        'name': value.name,
        'optionalUrlParams': value.optionalUrlParams,
        'requiredUrlParams': value.requiredUrlParams,
        'scopeDelimiter': value.scopeDelimiter,
        'scopes': value.scopes,
        'syncMappings': value.syncMappings,
        'tokenIntrospectionEndpoint': value.tokenIntrospectionEndpoint,
        'type': IdentityProviderTypeToJSON(value.type),
        'userLogoutEndpoint': value.userLogoutEndpoint,
    };
}

