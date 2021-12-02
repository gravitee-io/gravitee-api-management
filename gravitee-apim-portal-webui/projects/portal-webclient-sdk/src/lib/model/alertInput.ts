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
import { AlertType } from './alertType';
import { AlertTimeUnit } from './alertTimeUnit';


export interface AlertInput { 
    type?: AlertType;
    /**
     * Http status code to trigger the alert
     */
    status_code?: string;
    /**
     * Percent to trigger the alert on status code
     */
    status_percent?: number;
    /**
     * true, if alert is enabled
     */
    enabled?: boolean;
    /**
     * Response time to trigger the alert
     */
    response_time?: number;
    /**
     * Compute alert on selected duration
     */
    duration?: number;
    time_unit?: AlertTimeUnit;
}

