{{/*
Renders the distributed-sync block in gravitee.yml when gateway.distributedSync is a map with type.
When gateway.distributedSync is boolean true (legacy values), sync flags are enabled elsewhere and
Redis settings are expected from external gravitee.yml configuration.
*/}}
{{- define "gateway.distributedSync.graviteeYaml" -}}
{{- $ds := .Values.gateway.distributedSync -}}
{{- if and (kindIs "map" $ds) $ds.type -}}
    distributed-sync:
      type: {{ $ds.type }}
      {{- if eq $ds.type "redis" }}
      redis:
        host: {{ $ds.redis.host }}
        port: {{ $ds.redis.port }}
        {{- if $ds.redis.username }}
        username: {{ $ds.redis.username }}
        {{- end }}
        {{- if $ds.redis.password }}
        password: {{ $ds.redis.password }}
        {{- end }}
        {{- if $ds.redis.ssl }}
        ssl: {{ $ds.redis.ssl }}
        hostnameVerificationAlgorithm: {{ $ds.redis.hostnameVerificationAlgorithm | default "NONE" }}
        trustAll: {{ if hasKey $ds.redis "trustAll" }}{{ $ds.redis.trustAll }}{{else}}true{{end}}
        tlsProtocols: {{ $ds.redis.tlsProtocols | default "TLSv1.2" }}
        {{- if $ds.redis.tlsCiphers }}
        tlsCiphers: {{ $ds.redis.tlsCiphers | quote }}
        {{- end }}
        alpn: {{ $ds.redis.alpn | default false }}
        openssl: {{ $ds.redis.openssl | default false }}
        {{- end }}
        {{- if $ds.redis.keystore }}
        keystore:
          type: {{ $ds.redis.keystore.type | default "pem" }}
          path: {{ $ds.redis.keystore.path | default "${gravitee.home}/security/redis-keystore.jks" }}
          password: {{ required "The password required to access the keystore must be provided (i.e.kubernetes://{{NAMESPACE}}/secrets/{{SECRET_NAME}}/{{KEY_NAME}})" $ds.redis.keystore.password }}
          {{- if $ds.redis.keystore.keyPassword }}
          keyPassword: {{ $ds.redis.keystore.keyPassword | quote }}
          {{- end }}
          {{- if $ds.redis.keystore.alias }}
          alias: {{ $ds.redis.keystore.alias }}
          {{- end }}
          {{- if and ( eq $ds.redis.keystore.type "pem" ) ( empty $ds.redis.keystore.certificates ) }}
            {{ fail "When configuring Gravitee to use a keystore for mTLS, your certificates must be provided!" }}
          {{- end }}
          certificates:
            {{- range $ds.redis.keystore.certificates }}
            - cert: {{ .cert }}
              key: {{ .key }}
            {{- end }}
        {{- end}}
        {{- if $ds.redis.truststore }}
        truststore:
          type: {{ $ds.redis.truststore.type | default "pem" }}
          path: {{ $ds.redis.truststore.path | default "${gravitee.home}/security/redis-truststore.jks" }}
          password: {{ required "The password required to access the truststore must be provided! (i.e.kubernetes://{{NAMESPACE}}/secrets/{{SECRET_NAME}}/{{KEY_NAME}})" $ds.redis.truststore.password }}
          {{- if $ds.redis.truststore.alias }}
          alias: {{ $ds.redis.truststore.alias }}
          {{- end }}
        {{- end }}
        {{- if (not (empty (($ds.redis.sentinel).nodes))) }}
        sentinel:
          master: {{ $ds.redis.sentinel.master }}
          nodes:
            {{- range $ds.redis.sentinel.nodes }}
            - host: {{ .host }}
              port: {{ .port }}
            {{- end }}
        {{- end }}
        {{- if (not (empty ($ds.redis.operation))) }}
        operation:
          timeout: {{ $ds.redis.operation.timeout | default "10" }}
        {{- end }}
        {{- if (not (empty ($ds.redis.tcp))) }}
        tcp:
          connectTimeout: {{ $ds.redis.tcp.connectTimeout | default "5000" }}
          idleTimeout: {{ $ds.redis.tcp.idleTimeout | default "0" }}
        {{- end }}
      {{- end }}
{{- end -}}
{{- end -}}
