/* tslint:disable */
/* eslint-disable */
/**
 * Gravitee.io - Management API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
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
 * @interface SelectorHttpV4AllOf
 */
export interface SelectorHttpV4AllOf {
    /**
     * 
     * @type {string}
     * @memberof SelectorHttpV4AllOf
     */
    path?: string;
    /**
     * 
     * @type {string}
     * @memberof SelectorHttpV4AllOf
     */
    pathOperator?: SelectorHttpV4AllOfPathOperatorEnum;
    /**
     * 
     * @type {Array<string>}
     * @memberof SelectorHttpV4AllOf
     */
    methods?: Array<SelectorHttpV4AllOfMethodsEnum>;
}

export function SelectorHttpV4AllOfFromJSON(json: any): SelectorHttpV4AllOf {
    return SelectorHttpV4AllOfFromJSONTyped(json, false);
}

export function SelectorHttpV4AllOfFromJSONTyped(json: any, ignoreDiscriminator: boolean): SelectorHttpV4AllOf {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'path': !exists(json, 'path') ? undefined : json['path'],
        'pathOperator': !exists(json, 'pathOperator') ? undefined : json['pathOperator'],
        'methods': !exists(json, 'methods') ? undefined : json['methods'],
    };
}

export function SelectorHttpV4AllOfToJSON(value?: SelectorHttpV4AllOf | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'path': value.path,
        'pathOperator': value.pathOperator,
        'methods': value.methods,
    };
}

/**
* @export
* @enum {string}
*/
export enum SelectorHttpV4AllOfPathOperatorEnum {
    STARTSWITH = 'STARTS_WITH',
    EQUALS = 'EQUALS'
}
/**
* @export
* @enum {string}
*/
export enum SelectorHttpV4AllOfMethodsEnum {
    CONNECT = 'CONNECT',
    DELETE = 'DELETE',
    GET = 'GET',
    HEAD = 'HEAD',
    OPTIONS = 'OPTIONS',
    PATCH = 'PATCH',
    POST = 'POST',
    PUT = 'PUT',
    TRACE = 'TRACE',
    OTHER = 'OTHER'
}


