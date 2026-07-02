{{/*
Gateway helpers split:

Two helper families exist for every gateway scope (top-level, services.bridge,
services.core/technical):

1. STRICT HTTPRoute helpers (gateway.<scope>.httpRoute.*)
   Used ONLY by the corresponding HTTPRoute manifest.
   They read ONLY from the httpRoute block.
   Required fields fail loudly when missing — no fallback to ingress.

2. EFFECTIVE route helpers (gateway.<scope>.route.*)
   Used by sibling components that need to know the effective host/path.
   Precedence is INGRESS-FIRST:
     - If <scope>.ingress.enabled=true  → use ingress values
     - Else if <scope>.httpRoute.enabled=true → use httpRoute values
     - Else → safe default

Per-server gateway routes (gateway.servers[i]) are not factored into helpers
because the range index cannot be passed cleanly to a Helm template; the same
strict rule is applied inline in gateway-httproute.yaml using `required`.
*/}}

{{/* ============================================================ */}}
{{/* Strict HTTPRoute helpers (no ingress fallback, fail on missing) */}}
{{/* ============================================================ */}}

{{- define "gateway.httpRoute.path" -}}
{{- $hr := default dict .Values.gateway.httpRoute -}}
{{- required "gateway.httpRoute.path is required when gateway.httpRoute.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "gateway.httpRoute.pathMatchType" -}}
{{- $hr := default dict .Values.gateway.httpRoute -}}
{{- required "gateway.httpRoute.pathMatchType is required when gateway.httpRoute.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "gateway.httpRoute.hosts" -}}
{{- $hr := default dict .Values.gateway.httpRoute -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "gateway.httpRoute.hostnames must contain at least one hostname when gateway.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "gateway.bridge.httpRoute.path" -}}
{{- $hr := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- required "gateway.services.bridge.httpRoute.path is required when gateway.services.bridge.httpRoute.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "gateway.bridge.httpRoute.pathMatchType" -}}
{{- $hr := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- required "gateway.services.bridge.httpRoute.pathMatchType is required when gateway.services.bridge.httpRoute.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "gateway.bridge.httpRoute.hosts" -}}
{{- $hr := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "gateway.services.bridge.httpRoute.hostnames must contain at least one hostname when gateway.services.bridge.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "gateway.technical.httpRoute.path" -}}
{{- $hr := default dict .Values.gateway.services.core.httpRoute -}}
{{- required "gateway.services.core.httpRoute.path is required when gateway.services.core.httpRoute.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "gateway.technical.httpRoute.pathMatchType" -}}
{{- $hr := default dict .Values.gateway.services.core.httpRoute -}}
{{- required "gateway.services.core.httpRoute.pathMatchType is required when gateway.services.core.httpRoute.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "gateway.technical.httpRoute.hosts" -}}
{{- $hr := default dict .Values.gateway.services.core.httpRoute -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "gateway.services.core.httpRoute.hostnames must contain at least one hostname when gateway.services.core.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{/* ============================================================ */}}
{{/* Effective route helpers (ingress-first, used by sibling components) */}}
{{/* ============================================================ */}}

{{/* --- Top-level gateway --- */}}

{{- define "gateway.route.path" -}}
{{- $ingress := default dict .Values.gateway.ingress -}}
{{- $httpRoute := default dict .Values.gateway.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/" (get $httpRoute "path") -}}
{{- else -}}
/
{{- end -}}
{{- end -}}

{{- define "gateway.route.hosts" -}}
{{- $ingress := default dict .Values.gateway.ingress -}}
{{- $httpRoute := default dict .Values.gateway.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{/* --- Bridge (gateway.services.bridge) --- */}}

{{- define "gateway.bridge.route.path" -}}
{{- $ingress := default dict .Values.gateway.services.bridge.ingress -}}
{{- $httpRoute := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/_bridge" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/_bridge" (get $httpRoute "path") -}}
{{- else -}}
/_bridge
{{- end -}}
{{- end -}}

{{- define "gateway.bridge.route.hosts" -}}
{{- $ingress := default dict .Values.gateway.services.bridge.ingress -}}
{{- $httpRoute := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{/* --- Technical (gateway.services.core) --- */}}

{{- define "gateway.technical.route.path" -}}
{{- $ingress := default dict .Values.gateway.services.core.ingress -}}
{{- $httpRoute := default dict .Values.gateway.services.core.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/" (get $httpRoute "path") -}}
{{- else -}}
/
{{- end -}}
{{- end -}}

{{- define "gateway.technical.route.hosts" -}}
{{- $ingress := default dict .Values.gateway.services.core.ingress -}}
{{- $httpRoute := default dict .Values.gateway.services.core.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{- define "gateway.route.schemeForRoute" -}}
{{- $route := default dict .route -}}
{{- default "https" (get $route "scheme") -}}
{{- end -}}

{{/*
Builds the portal entrypoint URL from gateway routing configuration.
Logic:
- Phase 1: iterate gateway.servers[] — for each server, check ingress first (if enabled with hosts),
  then httpRoute (if enabled with hostnames). First match wins.
- Phase 2: check top-level gateway.ingress (if enabled with hosts).
- Phase 3: check top-level gateway.httpRoute (if enabled with hostnames).
Returns the URL as https://host/path or empty string if no route is found.
*/}}
{{/*
Hazelcast cluster name for gateway node clustering (distributed sync primary election).
Defaults to "<release-fullname>-<sharding-tags-slug>" so clusterId changes when sharding_tags change.
Override with gateway.cluster.hazelcast.clusterName when needed.
Redis distributed-event keys are scoped by runtime clusterId derived from this name.
*/}}
{{- define "gateway.distributedSync.enabled" -}}
{{- $ds := .Values.gateway.distributedSync -}}
{{- if kindIs "bool" $ds -}}
{{- if $ds -}}true{{- end -}}
{{- else if kindIs "map" $ds -}}
{{- if not (hasKey $ds "enabled") -}}true
{{- else if $ds.enabled -}}true
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Fail when distributed sync is enabled without gateway Hazelcast clustering.
Included from always-rendered templates (e.g. deployment) so external-config installs are covered.
*/}}
{{- define "gateway.distributedSync.validate" -}}
{{- if include "gateway.distributedSync.enabled" . -}}
{{- if ne (dig "cluster" "type" "" .Values.gateway) "hazelcast" -}}
{{- fail "gateway.cluster.type must be set to hazelcast when gateway.distributedSync is enabled (cluster-scoped Redis requires a dedicated Hazelcast cluster per gateway Helm release)" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "gateway.cluster.hazelcast.clusterName" -}}
{{- if dig "cluster" "hazelcast" "clusterName" "" .Values.gateway -}}
{{- .Values.gateway.cluster.hazelcast.clusterName | trim | trunc 63 | trimSuffix "-" -}}
{{- else if .Values.gateway.sharding_tags -}}
{{- $tagsSlug := .Values.gateway.sharding_tags | lower | replace " " "" | replace "," "-" | replace "!" "not-" -}}
{{- $maxBaseLen := sub 63 (add (len $tagsSlug) 1) | int -}}
{{- if not (gt $maxBaseLen 0) -}}
{{- fail (printf "gateway.cluster.hazelcast.clusterName must be set explicitly: sharding_tags %q is too long for a derived Hazelcast cluster name (63 character limit)" .Values.gateway.sharding_tags) -}}
{{- end -}}
{{- $base := include "gravitee.gateway.fullname" . | trunc $maxBaseLen | trimSuffix "-" -}}
{{- $derived := printf "%s-%s" $base $tagsSlug -}}
{{- if gt (len $derived) 63 -}}
{{- fail (printf "gateway.cluster.hazelcast.clusterName must be set explicitly: cannot derive a unique cluster name for sharding_tags %q (release/fullname too long). Set gateway.cluster.hazelcast.clusterName" .Values.gateway.sharding_tags) -}}
{{- end -}}
{{- $derived -}}
{{- else -}}
{{- include "gravitee.gateway.fullname" . | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "gateway.route.portalEntrypointUrl" -}}
{{- $result := dict "url" "" -}}
{{- $servers := default (list) .Values.gateway.servers -}}
{{- range $server := $servers -}}
{{- if eq (default "" $server.type) "http" -}}
{{- $serverIngress := default dict $server.ingress -}}
{{- if and (eq (get $result "url") "") (default false $serverIngress.enabled) -}}
{{- $hosts := default (list) $serverIngress.hosts -}}
{{- if gt (len $hosts) 0 -}}
{{- $scheme := include "gateway.route.schemeForRoute" (dict "route" $serverIngress) -}}
{{- $_ := set $result "url" (printf "%s://%s%s" $scheme (index $hosts 0) (default "/" $serverIngress.path)) -}}
{{- end -}}
{{- end -}}
{{- $serverHttpRoute := default dict $server.httpRoute -}}
{{- if and (eq (get $result "url") "") (default false $serverHttpRoute.enabled) -}}
{{- $hostnames := default (list) $serverHttpRoute.hostnames -}}
{{- if gt (len $hostnames) 0 -}}
{{- $scheme := include "gateway.route.schemeForRoute" (dict "route" $serverHttpRoute) -}}
{{- $_ := set $result "url" (printf "%s://%s%s" $scheme (index $hostnames 0) (default "/" $serverHttpRoute.path)) -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{- if eq (len $servers) 0 -}}
{{- $gatewayIngress := default dict .Values.gateway.ingress -}}
{{- if and (eq (get $result "url") "") (default false (get $gatewayIngress "enabled")) -}}
{{- $hosts := default (list) (get $gatewayIngress "hosts") -}}
{{- if gt (len $hosts) 0 -}}
{{- $scheme := include "gateway.route.schemeForRoute" (dict "route" $gatewayIngress) -}}
{{- $_ := set $result "url" (printf "%s://%s%s" $scheme (index $hosts 0) (default "/" (get $gatewayIngress "path"))) -}}
{{- end -}}
{{- end -}}
{{- $gatewayHttpRoute := default dict .Values.gateway.httpRoute -}}
{{- if and (eq (get $result "url") "") (default false (get $gatewayHttpRoute "enabled")) -}}
{{- $hostnames := default (list) (get $gatewayHttpRoute "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{- $scheme := include "gateway.route.schemeForRoute" (dict "route" $gatewayHttpRoute) -}}
{{- $_ := set $result "url" (printf "%s://%s%s" $scheme (index $hostnames 0) (default "/" (get $gatewayHttpRoute "path"))) -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{- get $result "url" -}}
{{- end -}}
