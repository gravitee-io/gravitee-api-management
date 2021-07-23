# Gravitee File Reporter

[![Build Status](https://ci.gravitee.io/buildStatus/icon?job=gravitee-io/gravitee-reporter-file/master)](https://ci.gravitee.io/job/gravitee-io/job/gravitee-reporter-file/job/master/)

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
`fileName`     | File name to write access logs (use '%s-yyyy_mm_dd' pattern to create one file per event type on daily base)                       | *#{systemProperties['gravitee.home']}/metrics/%s-yyyy_mm_dd}*
`output`       | Type of output file (json, message_pack, elasticsearch, csv) | *json*
`flushInterval` | interval between file flush (in ms)  | *1000*
`retainDays`   | The number of days to retain files before deleting one | *0 (to retain forever)*
