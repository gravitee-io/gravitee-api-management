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
	Timestamp       time.Time `json:"timestamp"`
	Type            EventType `json:"type"`
	DeviceID        string    `json:"device_id"`
	Model           string    `json:"model,omitempty"`
	TokensIn        int       `json:"tokens_in,omitempty"`
	TokensInSystem  int       `json:"tokens_in_system,omitempty"`
	TokensInHistory int       `json:"tokens_in_history,omitempty"`
	TokensInUser    int       `json:"tokens_in_user,omitempty"`
	TokensOut       int       `json:"tokens_out,omitempty"`
	CostUSD         float64   `json:"cost_usd,omitempty"`
	LatencyMs       int64     `json:"latency_ms,omitempty"`
	PolicyApplied   string    `json:"policy_applied,omitempty"`
	Action          string    `json:"action,omitempty"`
	Reason          string    `json:"reason,omitempty"`
	Tool            string    `json:"tool,omitempty"`
	ProcessName     string    `json:"process_name,omitempty"`
	ProcessPID      int       `json:"process_pid,omitempty"`
	Provider        string    `json:"provider,omitempty"`
}

type DeviceState struct {
	Hostname  string    `json:"hostname"`
	LastSeen  time.Time `json:"last_seen"`
	ProxyPort int       `json:"proxy_port,omitempty"`
	Version   string    `json:"version"`
}

type Stats struct {
	RequestsTotal   int
	RequestsBlocked int
	TokensIn        int
	TokensOut       int
	TotalCostUSD    float64
}

type Collector struct {
	hostname  string
	deviceDir string
	mu        sync.Mutex
	file      *os.File
	stats     Stats
}

func NewCollector(hostname, baseDir string) *Collector {
	base := expandHome(baseDir)
	deviceDir := filepath.Join(base, hostname)

	if err := os.MkdirAll(deviceDir, 0o755); err != nil {
		fmt.Fprintf(os.Stderr, "failed to create metrics dir: %v\n", err)
	}

	eventsPath := filepath.Join(deviceDir, "events.jsonl")
	f, err := os.OpenFile(eventsPath, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
	if err != nil {
		fmt.Fprintf(os.Stderr, "failed to open metrics file: %v\n", err)
		return &Collector{hostname: hostname, deviceDir: deviceDir}
	}

	c := &Collector{hostname: hostname, deviceDir: deviceDir, file: f}
	c.writeDeviceState()
	go c.heartbeat()
	return c
}

func (c *Collector) heartbeat() {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()
	for range ticker.C {
		c.mu.Lock()
		c.writeDeviceState()
		c.mu.Unlock()
	}
}

func (c *Collector) writeDeviceState() {
	state := DeviceState{
		Hostname: c.hostname,
		LastSeen: time.Now().UTC(),
		Version:  "1.0.0",
	}
	data, err := json.MarshalIndent(state, "", "  ")
	if err != nil {
		return
	}
	statePath := filepath.Join(c.deviceDir, "device.json")
	os.WriteFile(statePath, data, 0o644)
}

func (c *Collector) Record(event Event) {
	if event.Timestamp.IsZero() {
		event.Timestamp = time.Now().UTC()
	}
	if event.DeviceID == "" {
		event.DeviceID = c.hostname
	}

	c.mu.Lock()
	defer c.mu.Unlock()

	c.writeDeviceState()
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
