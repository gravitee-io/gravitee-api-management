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
import { Group } from './group';
import { User } from './user';
import { ApplicationLinks } from './applicationLinks';


export interface Application { 
    /**
     * Unique identifier of an application.
     */
    id?: string;
    /**
     * Name of the application.
     */
    name?: string;
    /**
     * Description of the application.
     */
    description?: string;
    /**
     * Domain used by the application.
     */
    domain?: string;
    /**
     * Type of the application (Web, Mobile, ...).
     */
    applicationType?: string;
    /**
     * True if the application has client id.
     */
    hasClientId?: boolean;
    owner?: User;
    /**
     * Creation date and time of the application.
     */
    created_at?: Date;
    /**
     * Last update date and time of the application.
     */
    updated_at?: Date;
    /**
     * Array of groups associated to the application.
     */
    groups?: Array<Group>;
    /**
     * Picture of the application. This attribute is only used to update a picture.\\ To get the application picture, use /application/{applicationId}/picture. 
     */
    picture?: string;
    /**
     * Background of the application. This attribute is only used to update a picture.\\ To get the application picture, use /application/{applicationId}/background. 
     */
    background?: string;
    settings?: ApplicationSettings;
    _links?: ApplicationLinks;
}

