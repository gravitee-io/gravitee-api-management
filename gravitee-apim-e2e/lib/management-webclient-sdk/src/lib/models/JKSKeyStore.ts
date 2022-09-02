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
import type { JKSKeyStoreAllOf } from './JKSKeyStoreAllOf';
import {
    JKSKeyStoreAllOfFromJSON,
    JKSKeyStoreAllOfFromJSONTyped,
    JKSKeyStoreAllOfToJSON,
} from './JKSKeyStoreAllOf';
import type { KeyStore } from './KeyStore';
import {
    KeyStoreFromJSON,
    KeyStoreFromJSONTyped,
    KeyStoreToJSON,
} from './KeyStore';

/**
 * 
 * @export
 * @interface JKSKeyStore
 */
export interface JKSKeyStore extends KeyStore {
    /**
     * 
     * @type {string}
     * @memberof JKSKeyStore
     */
    path?: string;
    /**
     * 
     * @type {string}
     * @memberof JKSKeyStore
     */
    content?: string;
    /**
     * 
     * @type {string}
     * @memberof JKSKeyStore
     */
    password?: string;
}



/**
 * Check if a given object implements the JKSKeyStore interface.
 */
export function instanceOfJKSKeyStore(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function JKSKeyStoreFromJSON(json: any): JKSKeyStore {
    return JKSKeyStoreFromJSONTyped(json, false);
}

export function JKSKeyStoreFromJSONTyped(json: any, ignoreDiscriminator: boolean): JKSKeyStore {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        ...KeyStoreFromJSONTyped(json, ignoreDiscriminator),
        'path': !exists(json, 'path') ? undefined : json['path'],
        'content': !exists(json, 'content') ? undefined : json['content'],
        'password': !exists(json, 'password') ? undefined : json['password'],
    };
}

export function JKSKeyStoreToJSON(value?: JKSKeyStore | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        ...KeyStoreToJSON(value),
        'path': value.path,
        'content': value.content,
        'password': value.password,
    };
}

