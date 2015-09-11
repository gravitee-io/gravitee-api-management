# Gravitee Reporter Access Log

[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gravitee-reporter-accesslog)](http://build.gravitee.io/jenkins/job/gravitee-reporter-accesslog/)

Presentation
------------

This reporter write access logs to a file, by corresponding one line to one request access to gateway.

Line format
-----------

For now, line format is defined as the following:

    [TIMESTAMP] REMOTE_IP LOCAL_IP METHOD PATH STATUS LENGTH

Where:

- `TIMESTAMP` is the timestamp from which request began to be processed by gateway;
- `REMOTE_IP` is the remote host's IP;
- `LOCAL_IP` is the local Gravitee node's IP;
- `METHOD` is the HTTP method contained into the request;
- `PATH` is the HTTP path contained into the request;
- `STATUS` is the HTTP response status received from the API;
- `LENGTH` is the HTTP `Content-Length` value received from the API.
 

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