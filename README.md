# Gravitee File Reporter

[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gravitee-reporter-file)](http://build.gravitee.io/jenkins/job/gravitee-reporter-file/)

Presentation
------------

This reporter write access logs to a file, by corresponding one line to one request access to gateway.

Line format
-----------

For now, line format is defined as the following:

    [TIMESTAMP] (LOCAL_IP) REMOTE_IP API KEY METHOD PATH STATUS LENGTH TOTAL_RESPONSE_TIME

Where:

- `TIMESTAMP` is the timestamp from which request began to be processed by gateway;
- `LOCAL_IP` is the local host's IP, i.e., the Gravitee *node*'s IP which have processed request;
- `REMOTE_IP` is the remote host's IP;
- `API` is the requested API name;
- `KEY` is the key used to request to API;
- `METHOD` is the HTTP method contained into the request;
- `PATH` is the HTTP path contained into the request;
- `STATUS` is the HTTP response status received from the API;
- `LENGTH` is the HTTP `Content-Length` value received from the API.
- `TOTAL_RESPONSE_TIME` is the total response time to process request (API + *gateway* response time).
 

Configuration
-------------

Hereafter available configuration parameters: 

Parameter name | Description                                            | Default value
---------------|--------------------------------------------------------|--------------
`fileName`     | File name to write access logs                         | *access-yy_mm_dd.log*
`append`       | Append to the existing file is necessary               | *true*
`retainDays`   | The number of days to retain files before deleting one | *0 (to retain forever)*
`dateFormat`   | The date format used to write access logs              | *yyyy_MM_dd*
`backupFormat` | The format for the file extension of backup files      | *HHmmssSSS*
