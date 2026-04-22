{{/*
Gateway route helpers — mirrors the api-helpers.tpl and ui-helpers.tpl patterns.
Precedence: when ingress is enabled, use ingress values; otherwise use httpRoute values.
*/}}

{{/*
Builds the portal entrypoint URL from gateway routing configuration.
Logic:
- Phase 1: iterate gateway.servers[] — for each server, check ingress first (if enabled with hosts),
  then httpRoute (if enabled with hostnames). First match wins.
- Phase 2: check top-level gateway.ingress (if enabled with hosts).
- Phase 3: check top-level gateway.httpRoute (if enabled with hostnames).
Returns the URL as https://host/path or empty string if no route is found.
*/}}
{{- define "gateway.route.portalEntrypointUrl" -}}
{{- $result := dict "url" "" -}}
{{/* Phase 1: servers[] — ingress then httpRoute per server */}}
{{- range $server := default (list) .Values.gateway.servers -}}
{{- $serverIngress := default dict $server.ingress -}}
{{- if and (eq (get $result "url") "") (default false $serverIngress.enabled) -}}
{{- $hosts := default (list) $serverIngress.hosts -}}
{{- if gt (len $hosts) 0 -}}
{{- $_ := set $result "url" (printf "https://%s%s" (index $hosts 0) (default "/" $serverIngress.path)) -}}
{{- end -}}
{{- end -}}
{{- $serverHttpRoute := default dict $server.httpRoute -}}
{{- if and (eq (get $result "url") "") (default false $serverHttpRoute.enabled) -}}
{{- $hostnames := default (list) $serverHttpRoute.hostnames -}}
{{- if gt (len $hostnames) 0 -}}
{{- $_ := set $result "url" (printf "https://%s%s" (index $hostnames 0) (default "/" $serverHttpRoute.path)) -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{/* Phase 2: top-level gateway.ingress */}}
{{- $gatewayIngress := default dict .Values.gateway.ingress -}}
{{- if and (eq (get $result "url") "") (ne (get $gatewayIngress "enabled") false) -}}
{{- $hosts := default (list) (get $gatewayIngress "hosts") -}}
{{- if gt (len $hosts) 0 -}}
{{- $_ := set $result "url" (printf "https://%s%s" (index $hosts 0) (default "/" (get $gatewayIngress "path"))) -}}
{{- end -}}
{{- end -}}
{{/* Phase 3: top-level gateway.httpRoute */}}
{{- $gatewayHttpRoute := default dict .Values.gateway.httpRoute -}}
{{- if and (eq (get $result "url") "") (default false (get $gatewayHttpRoute "enabled")) -}}
{{- $hostnames := default (list) (get $gatewayHttpRoute "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{- $_ := set $result "url" (printf "https://%s%s" (index $hostnames 0) (default "/" (get $gatewayHttpRoute "path"))) -}}
{{- end -}}
{{- end -}}
{{- get $result "url" -}}
{{- end -}}
