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
/**
 * 
 * @export
 * @interface GroupMemberEntity
 */
export interface GroupMemberEntity {
    /**
     * 
     * @type {Date}
     * @memberof GroupMemberEntity
     */
    created_at?: Date;
    /**
     * 
     * @type {string}
     * @memberof GroupMemberEntity
     */
    displayName?: string;
    /**
     * 
     * @type {string}
     * @memberof GroupMemberEntity
     */
    id?: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof GroupMemberEntity
     */
    roles?: { [key: string]: string; };
    /**
     * 
     * @type {Date}
     * @memberof GroupMemberEntity
     */
    updated_at?: Date;
}

/**
 * Check if a given object implements the GroupMemberEntity interface.
 */
export function instanceOfGroupMemberEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GroupMemberEntityFromJSON(json: any): GroupMemberEntity {
    return GroupMemberEntityFromJSONTyped(json, false);
}

export function GroupMemberEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): GroupMemberEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'created_at': !exists(json, 'created_at') ? undefined : (new Date(json['created_at'])),
        'displayName': !exists(json, 'displayName') ? undefined : json['displayName'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'roles': !exists(json, 'roles') ? undefined : json['roles'],
        'updated_at': !exists(json, 'updated_at') ? undefined : (new Date(json['updated_at'])),
    };
}

export function GroupMemberEntityToJSON(value?: GroupMemberEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'created_at': value.created_at === undefined ? undefined : (value.created_at.toISOString()),
        'displayName': value.displayName,
        'id': value.id,
        'roles': value.roles,
        'updated_at': value.updated_at === undefined ? undefined : (value.updated_at.toISOString()),
    };
}

