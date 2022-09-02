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
 * @interface GroupMappingEntity
 */
export interface GroupMappingEntity {
    /**
     * 
     * @type {string}
     * @memberof GroupMappingEntity
     */
    condition?: string;
    /**
     * 
     * @type {Array<string>}
     * @memberof GroupMappingEntity
     */
    groups: Array<string>;
}

/**
 * Check if a given object implements the GroupMappingEntity interface.
 */
export function instanceOfGroupMappingEntity(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "groups" in value;

    return isInstance;
}

export function GroupMappingEntityFromJSON(json: any): GroupMappingEntity {
    return GroupMappingEntityFromJSONTyped(json, false);
}

export function GroupMappingEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): GroupMappingEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'condition': !exists(json, 'condition') ? undefined : json['condition'],
        'groups': json['groups'],
    };
}

export function GroupMappingEntityToJSON(value?: GroupMappingEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'condition': value.condition,
        'groups': value.groups,
    };
}

