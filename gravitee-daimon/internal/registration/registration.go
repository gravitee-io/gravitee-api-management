package registration

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/user"
	"runtime"
	"time"

	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/tui"
)

type Client struct {
	gatewayURL   string
	heartbeatSec int
	deviceID     string
	httpClient   *http.Client
}

type registerRequest struct {
	DeviceID     string   `json:"deviceId"`
	Hostname     string   `json:"hostname"`
	User         string   `json:"user"`
	Version      string   `json:"version"`
	OS           string   `json:"os"`
	Capabilities []string `json:"capabilities"`
}

type heartbeatRequest struct {
	DeviceID  string         `json:"deviceId"`
	Timestamp time.Time      `json:"timestamp"`
	UptimeSec int64          `json:"uptimeSec"`
	Stats     heartbeatStats `json:"stats"`
}

type heartbeatStats struct {
	RequestsTotal   int `json:"requestsTotal"`
	RequestsBlocked int `json:"requestsBlocked"`
	TokensTotal     int `json:"tokensTotal"`
}

func NewClient(gatewayURL string, heartbeatSec int) *Client {
	return &Client{
		gatewayURL:   gatewayURL,
		heartbeatSec: heartbeatSec,
		deviceID:     generateDeviceID(),
		httpClient:   &http.Client{Timeout: 10 * time.Second},
	}
}

func (c *Client) DeviceID() string {
	return c.deviceID
}

func (c *Client) Start(ctx context.Context, events chan<- tui.Event) {
	if err := c.register(); err != nil {
		log.Printf("registration failed: %v", err)
		events <- tui.Event{
			Type:   tui.EventRegistration,
			Time:   time.Now(),
			Reason: fmt.Sprintf("registration failed: %v", err),
		}
	} else {
		events <- tui.Event{
			Type:   tui.EventRegistration,
			Time:   time.Now(),
			Reason: "registered successfully",
		}
	}

	startTime := time.Now()
	ticker := time.NewTicker(time.Duration(c.heartbeatSec) * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			uptime := int64(time.Since(startTime).Seconds())
			if err := c.heartbeat(uptime); err != nil {
				log.Printf("heartbeat failed: %v", err)
			}
		}
	}
}

func (c *Client) register() error {
	hostname, _ := os.Hostname()
	u, _ := user.Current()

	req := registerRequest{
		DeviceID:     c.deviceID,
		Hostname:     hostname,
		User:         u.Username,
		Version:      "0.1.0",
		OS:           fmt.Sprintf("%s/%s", runtime.GOOS, runtime.GOARCH),
		Capabilities: []string{"proxy", "policy-engine", "detector"},
	}

	return c.post("/daimon/register", req)
}

func (c *Client) heartbeat(uptimeSec int64) error {
	req := heartbeatRequest{
		DeviceID:  c.deviceID,
		Timestamp: time.Now().UTC(),
		UptimeSec: uptimeSec,
	}

	return c.post("/daimon/heartbeat", req)
}

func (c *Client) post(path string, body any) error {
	data, err := json.Marshal(body)
	if err != nil {
		return err
	}

	resp, err := c.httpClient.Post(c.gatewayURL+path, "application/json", bytes.NewReader(data))
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		return fmt.Errorf("gateway returned %d", resp.StatusCode)
	}
	return nil
}

func generateDeviceID() string {
	hostname, _ := os.Hostname()
	mac := getMACAddress()
	hash := sha256.Sum256([]byte(hostname + mac))
	return fmt.Sprintf("d-%x", hash[:4])
}

func getMACAddress() string {
	interfaces, err := net.Interfaces()
	if err != nil {
		return "unknown"
	}
	for _, iface := range interfaces {
		if iface.Flags&net.FlagUp != 0 && iface.Flags&net.FlagLoopback == 0 {
			addr := iface.HardwareAddr.String()
			if addr != "" {
				return addr
			}
		}
	}
	return "unknown"
}
