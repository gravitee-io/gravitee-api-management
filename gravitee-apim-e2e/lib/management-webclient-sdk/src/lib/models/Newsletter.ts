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
 * @interface Newsletter
 */
export interface Newsletter {
    /**
     * 
     * @type {boolean}
     * @memberof Newsletter
     */
    enabled?: boolean;
}

/**
 * Check if a given object implements the Newsletter interface.
 */
export function instanceOfNewsletter(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function NewsletterFromJSON(json: any): Newsletter {
    return NewsletterFromJSONTyped(json, false);
}

export function NewsletterFromJSONTyped(json: any, ignoreDiscriminator: boolean): Newsletter {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'enabled': !exists(json, 'enabled') ? undefined : json['enabled'],
    };
}

export function NewsletterToJSON(value?: Newsletter | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'enabled': value.enabled,
    };
}

