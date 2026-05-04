package proxy

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"
	"time"

	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/metrics"
	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/policy"
	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/tui"
)

type Proxy struct {
	target    *url.URL
	engine    *policy.Engine
	collector *metrics.Collector
	events    chan<- tui.Event
	reverse   *httputil.ReverseProxy
}

type anthropicRequest struct {
	Model string `json:"model"`
}

func New(targetURL string, engine *policy.Engine, collector *metrics.Collector, events chan<- tui.Event) *Proxy {
	target, err := url.Parse(targetURL)
	if err != nil {
		log.Fatalf("invalid target URL: %v", err)
	}

	p := &Proxy{
		target:    target,
		engine:    engine,
		collector: collector,
		events:    events,
	}

	p.reverse = &httputil.ReverseProxy{
		Director: func(req *http.Request) {
			req.URL.Scheme = target.Scheme
			req.URL.Host = target.Host
			req.Host = target.Host
			// SDK sends /v1/..., rewrite to gateway API path /ai/v1/...
			req.URL.Path = target.Path + strings.TrimPrefix(req.URL.Path, "/v1")
		},
	}

	return p
}

func (p *Proxy) Start(port int) {
	mux := http.NewServeMux()
	mux.HandleFunc("/", p.handleRequest)
	addr := fmt.Sprintf(":%d", port)
	log.Printf("proxy listening on %s", addr)
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("proxy server failed: %v", err)
	}
}

func (p *Proxy) handleRequest(w http.ResponseWriter, r *http.Request) {
	start := time.Now()

	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "failed to read request body", http.StatusBadRequest)
		return
	}
	r.Body = io.NopCloser(bytes.NewReader(body))

	var ar anthropicRequest
	json.Unmarshal(body, &ar)

	req := &policy.InterceptedRequest{
		Model: ar.Model,
		Body:  body,
	}

	result := p.engine.Evaluate(r.Context(), req)

	tool := detectTool(r.Header.Get("User-Agent"))

	if result.Action == policy.ActionBlock {
		latency := time.Since(start).Milliseconds()

		p.collector.Record(metrics.Event{
			Type:          metrics.EventPolicyBlock,
			Model:         ar.Model,
			PolicyApplied: result.Policy,
			Action:        string(result.Action),
			Reason:        result.Reason,
			LatencyMs:     latency,
			Tool:          tool,
		})

		p.events <- tui.Event{
			Type:    tui.EventBlocked,
			Time:    time.Now(),
			Model:   ar.Model,
			Policy:  result.Policy,
			Reason:  result.Reason,
			Tool:    tool,
			Latency: time.Duration(latency) * time.Millisecond,
		}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]any{
			"type": "error",
			"error": map[string]string{
				"type":    "policy_violation",
				"message": result.Reason,
			},
		})
		return
	}

	rec := &responseRecorder{ResponseWriter: w, statusCode: http.StatusOK}
	p.reverse.ServeHTTP(rec, r)

	latency := time.Since(start).Milliseconds()

	p.collector.Record(metrics.Event{
		Type:      metrics.EventRequest,
		Model:     ar.Model,
		TokensIn:  estimateInputTokens(body),
		TokensOut: estimateOutputTokens(rec.body.Bytes()),
		CostUSD:   0, // TODO: calculate from model pricing
		LatencyMs: latency,
		Action:    "allowed",
		Tool:      tool,
	})

	p.events <- tui.Event{
		Type:     tui.EventRequest,
		Time:     time.Now(),
		Model:    ar.Model,
		TokensIn: estimateInputTokens(body),
		TokenOut: estimateOutputTokens(rec.body.Bytes()),
		Latency:  time.Duration(latency) * time.Millisecond,
		Tool:     tool,
	}
}

type responseRecorder struct {
	http.ResponseWriter
	statusCode int
	body       bytes.Buffer
}

func (rr *responseRecorder) Write(b []byte) (int, error) {
	rr.body.Write(b)
	return rr.ResponseWriter.Write(b)
}

func (rr *responseRecorder) WriteHeader(code int) {
	rr.statusCode = code
	rr.ResponseWriter.WriteHeader(code)
}

func detectTool(userAgent string) string {
	switch {
	case contains(userAgent, "claude-code"):
		return "claude-code"
	case contains(userAgent, "cursor"):
		return "cursor"
	default:
		return "unknown"
	}
}

func contains(s, substr string) bool {
	return len(s) >= len(substr) && (s == substr || len(s) > 0 && containsLower(s, substr))
}

func containsLower(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}

func estimateInputTokens(body []byte) int {
	return len(body) / 4
}

func estimateOutputTokens(body []byte) int {
	return len(body) / 4
}
