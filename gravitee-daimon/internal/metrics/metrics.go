package metrics

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

type EventType string

const (
	EventRequest          EventType = "request"
	EventPolicyBlock      EventType = "policy_block"
	EventPolicyWarn       EventType = "policy_warn"
	EventDirectConnection EventType = "direct_connection"
)

type Event struct {
	Timestamp     time.Time `json:"timestamp"`
	Type          EventType `json:"type"`
	DeviceID      string    `json:"device_id"`
	Model         string    `json:"model,omitempty"`
	TokensIn      int       `json:"tokens_in,omitempty"`
	TokensOut     int       `json:"tokens_out,omitempty"`
	CostUSD       float64   `json:"cost_usd,omitempty"`
	LatencyMs     int64     `json:"latency_ms,omitempty"`
	PolicyApplied string    `json:"policy_applied,omitempty"`
	Action        string    `json:"action,omitempty"`
	Reason        string    `json:"reason,omitempty"`
	Tool          string    `json:"tool,omitempty"`
	ProcessName   string    `json:"process_name,omitempty"`
	ProcessPID    int       `json:"process_pid,omitempty"`
	Provider      string    `json:"provider,omitempty"`
}

type Stats struct {
	RequestsTotal   int
	RequestsBlocked int
	TokensIn        int
	TokensOut       int
	TotalCostUSD    float64
}

type Collector struct {
	mu    sync.Mutex
	file  *os.File
	stats Stats
}

func NewCollector(outputPath string) *Collector {
	expanded := expandHome(outputPath)
	if err := os.MkdirAll(filepath.Dir(expanded), 0o755); err != nil {
		fmt.Fprintf(os.Stderr, "failed to create metrics dir: %v\n", err)
	}

	f, err := os.OpenFile(expanded, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
	if err != nil {
		fmt.Fprintf(os.Stderr, "failed to open metrics file: %v\n", err)
		return &Collector{}
	}

	return &Collector{file: f}
}

func (c *Collector) Record(event Event) {
	if event.Timestamp.IsZero() {
		event.Timestamp = time.Now().UTC()
	}

	c.mu.Lock()
	defer c.mu.Unlock()

	c.stats.RequestsTotal++
	if event.Action == "blocked" {
		c.stats.RequestsBlocked++
	}
	c.stats.TokensIn += event.TokensIn
	c.stats.TokensOut += event.TokensOut
	c.stats.TotalCostUSD += event.CostUSD

	if c.file != nil {
		data, err := json.Marshal(event)
		if err == nil {
			c.file.Write(data)
			c.file.Write([]byte("\n"))
		}
	}
}

func (c *Collector) Stats() Stats {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.stats
}

func (c *Collector) Close() {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.file != nil {
		c.file.Close()
	}
}

func expandHome(path string) string {
	if strings.HasPrefix(path, "~/") {
		home, _ := os.UserHomeDir()
		return filepath.Join(home, path[2:])
	}
	return path
}
