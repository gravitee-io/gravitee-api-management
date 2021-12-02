/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * The version of the OpenAPI document: 3.14.0-SNAPSHOT
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { ApplicationSettings } from './applicationSettings';


export interface ApplicationInput { 
    name?: string;
    description?: string;
    domain?: string;
    picture?: string;
    groups?: Array<string>;
    settings?: ApplicationSettings;
    background?: string;
}

