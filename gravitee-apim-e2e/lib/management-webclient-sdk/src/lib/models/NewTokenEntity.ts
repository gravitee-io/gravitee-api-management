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
 * @interface NewTokenEntity
 */
export interface NewTokenEntity {
    /**
     * 
     * @type {string}
     * @memberof NewTokenEntity
     */
    name: string;
}

/**
 * Check if a given object implements the NewTokenEntity interface.
 */
export function instanceOfNewTokenEntity(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function NewTokenEntityFromJSON(json: any): NewTokenEntity {
    return NewTokenEntityFromJSONTyped(json, false);
}

export function NewTokenEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): NewTokenEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'name': json['name'],
    };
}

export function NewTokenEntityToJSON(value?: NewTokenEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'name': value.name,
    };
}

