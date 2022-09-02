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
 * @interface EntrypointV4
 */
export interface EntrypointV4 {
    /**
     * 
     * @type {any}
     * @memberof EntrypointV4
     */
    configuration?: any;
    /**
     * 
     * @type {string}
     * @memberof EntrypointV4
     */
    type: string;
}

/**
 * Check if a given object implements the EntrypointV4 interface.
 */
export function instanceOfEntrypointV4(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "type" in value;

    return isInstance;
}

export function EntrypointV4FromJSON(json: any): EntrypointV4 {
    return EntrypointV4FromJSONTyped(json, false);
}

export function EntrypointV4FromJSONTyped(json: any, ignoreDiscriminator: boolean): EntrypointV4 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'configuration': !exists(json, 'configuration') ? undefined : json['configuration'],
        'type': json['type'],
    };
}

export function EntrypointV4ToJSON(value?: EntrypointV4 | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'configuration': value.configuration,
        'type': value.type,
    };
}

