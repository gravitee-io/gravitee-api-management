{{/*
API helper split:
- api.management.httpRoute.* and api.portal.httpRoute.* are strict helpers for HTTPRoute manifests only
- api.management.route.* and api.portal.route.* are shared discovery helpers for runtime URLs and config
*/}}

{{/* --- Management strict HTTPRoute helpers --- */}}

{{- define "api.management.httpRoute.path" -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $managementHttpRoute := default dict (get $apiHttpRoute "management") -}}
{{- required "api.httpRoute.management.path is required when api.httpRoute.management.enabled=true" (get $managementHttpRoute "path") -}}
{{- end -}}

{{- define "api.management.httpRoute.hosts" -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $managementHttpRoute := default dict (get $apiHttpRoute "management") -}}
{{- $hostnames := default (list) (get $managementHttpRoute "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "api.httpRoute.management.hostnames must contain at least one hostname when api.httpRoute.management.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "api.management.httpRoute.pathMatchType" -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $managementHttpRoute := default dict (get $apiHttpRoute "management") -}}
{{- required "api.httpRoute.management.pathMatchType is required when api.httpRoute.management.enabled=true" (get $managementHttpRoute "pathMatchType") -}}
{{- end -}}

{{/* --- Management active route helpers --- */}}


{{- define "api.management.route.path" -}}
{{- $management := default dict .Values.api.ingress.management -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $managementHttpRoute := default dict (get $apiHttpRoute "management") -}}
{{- if and (default false (get $management "enabled")) (get $management "path") -}}
{{- get $management "path" -}}
{{- else if and (default false (get $managementHttpRoute "enabled")) (get $managementHttpRoute "path") -}}
{{- get $managementHttpRoute "path" -}}
{{- else -}}
/management
{{- end -}}
{{- end -}}

{{- define "api.management.route.hosts" -}}
{{- $management := default dict .Values.api.ingress.management -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $managementHttpRoute := default dict (get $apiHttpRoute "management") -}}
{{- if and (default false (get $management "enabled")) (get $management "hosts") -}}
{{ toYaml (get $management "hosts") }}
{{- else if and (default false (get $managementHttpRoute "enabled")) (get $managementHttpRoute "hostnames") -}}
{{ toYaml (get $managementHttpRoute "hostnames") }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{- define "api.management.route.scheme" -}}
{{- $management := default dict .Values.api.ingress.management -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $managementHttpRoute := default dict (get $apiHttpRoute "management") -}}
{{- if and (default false (get $management "enabled")) -}}
{{- default "https" (get $management "scheme") -}}
{{- else if and (default false (get $managementHttpRoute "enabled")) -}}
{{- default "https" (get $managementHttpRoute "scheme") -}}
{{- else -}}
https
{{- end -}}
{{- end -}}


{{/*
Builds the full management API URL (scheme://host/path) for use in configmaps and other templates.
Logic:
- Gets the host(s) using the precedence: ingress hosts if enabled, else httpRoute hostnames if enabled, else empty.
- Gets the scheme (https/http) and path (e.g., /management) using the same precedence.
- If at least one host is present, outputs the URL as: scheme://host/path (using the first host).
- If no hosts, outputs nothing.
*/}}
{{- define "api.management.route.url" -}}
{{- $hosts := fromYamlArray (include "api.management.route.hosts" .) -}}
{{- $scheme := include "api.management.route.scheme" . -}}
{{- $path := include "api.management.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" $scheme (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]+" $path) }}
{{- end -}}
{{- end -}}

{{- define "api.management.route.pathMatchType" -}}
{{- $management := default dict .Values.api.ingress.management -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $managementHttpRoute := default dict (get $apiHttpRoute "management") -}}
{{- if and (default false (get $management "enabled")) -}}
  {{- $pt := default "Prefix" (get $management "pathType") -}}
  {{- if eq $pt "ImplementationSpecific" -}}RegularExpression{{- else if eq $pt "Prefix" -}}PathPrefix{{- else -}}{{ $pt }}{{- end -}}
{{- else if and (default false (get $managementHttpRoute "enabled")) (get $managementHttpRoute "pathMatchType") -}}
{{- get $managementHttpRoute "pathMatchType" -}}
{{- else -}}
PathPrefix
{{- end -}}
{{- end -}}

{{/* --- Portal strict HTTPRoute helpers --- */}}

{{- define "api.portal.httpRoute.path" -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $portalHttpRoute := default dict (get $apiHttpRoute "portal") -}}
{{- required "api.httpRoute.portal.path is required when api.httpRoute.portal.enabled=true" (get $portalHttpRoute "path") -}}
{{- end -}}

{{- define "api.portal.httpRoute.hosts" -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $portalHttpRoute := default dict (get $apiHttpRoute "portal") -}}
{{- $hostnames := default (list) (get $portalHttpRoute "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "api.httpRoute.portal.hostnames must contain at least one hostname when api.httpRoute.portal.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "api.portal.httpRoute.pathMatchType" -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $portalHttpRoute := default dict (get $apiHttpRoute "portal") -}}
{{- required "api.httpRoute.portal.pathMatchType is required when api.httpRoute.portal.enabled=true" (get $portalHttpRoute "pathMatchType") -}}
{{- end -}}

{{/* --- Portal active route helpers --- */}}

{{- define "api.portal.route.path" -}}
{{- $portal := default dict .Values.api.ingress.portal -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $portalHttpRoute := default dict (get $apiHttpRoute "portal") -}}
{{- if and (default false (get $portal "enabled")) (get $portal "path") -}}
{{- get $portal "path" -}}
{{- else if and (default false (get $portalHttpRoute "enabled")) (get $portalHttpRoute "path") -}}
{{- get $portalHttpRoute "path" -}}
{{- else -}}
/portal
{{- end -}}
{{- end -}}

{{- define "api.portal.route.hosts" -}}
{{- $portal := default dict .Values.api.ingress.portal -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $portalHttpRoute := default dict (get $apiHttpRoute "portal") -}}
{{- if and (default false (get $portal "enabled")) (get $portal "hosts") -}}
{{ toYaml (get $portal "hosts") }}
{{- else if and (default false (get $portalHttpRoute "enabled")) (get $portalHttpRoute "hostnames") -}}
{{ toYaml (get $portalHttpRoute "hostnames") }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{- define "api.portal.route.scheme" -}}
{{- $portal := default dict .Values.api.ingress.portal -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $portalHttpRoute := default dict (get $apiHttpRoute "portal") -}}
{{- if and (default false (get $portal "enabled")) -}}
{{- default "https" (get $portal "scheme") -}}
{{- else if and (default false (get $portalHttpRoute "enabled")) -}}
{{- default "https" (get $portalHttpRoute "scheme") -}}
{{- else -}}
https
{{- end -}}
{{- end -}}

{{- define "api.portal.route.url" -}}
{{- $hosts := fromYamlArray (include "api.portal.route.hosts" .) -}}
{{- $scheme := include "api.portal.route.scheme" . -}}
{{- $path := include "api.portal.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" $scheme (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]+" $path) }}
{{- end -}}
{{- end -}}

{{- define "api.portal.route.pathMatchType" -}}
{{- $portal := default dict .Values.api.ingress.portal -}}
{{- $apiHttpRoute := default dict .Values.api.httpRoute -}}
{{- $portalHttpRoute := default dict (get $apiHttpRoute "portal") -}}
{{- if and (default false (get $portal "enabled")) -}}
  {{- $pt := default "Prefix" (get $portal "pathType") -}}
  {{- if eq $pt "ImplementationSpecific" -}}RegularExpression{{- else if eq $pt "Prefix" -}}PathPrefix{{- else -}}{{ $pt }}{{- end -}}
{{- else if and (default false (get $portalHttpRoute "enabled")) (get $portalHttpRoute "pathMatchType") -}}
{{- get $portalHttpRoute "pathMatchType" -}}
{{- else -}}
PathPrefix
{{- end -}}
{{- end -}}
