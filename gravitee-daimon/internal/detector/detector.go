package detector

import (
	"bufio"
	"context"
	"fmt"
	"log"
	"os/exec"
	"strconv"
	"strings"
	"time"

	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/metrics"
	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/tui"
)

type DirectConnection struct {
	ProcessName string
	PID         int
	Provider    string
}

type Detector struct {
	providers   []string
	intervalSec int
	collector   *metrics.Collector
	events      chan<- tui.Event
}

func New(providers []string, intervalSec int, collector *metrics.Collector, events chan<- tui.Event) *Detector {
	return &Detector{
		providers:   providers,
		intervalSec: intervalSec,
		collector:   collector,
		events:      events,
	}
}

func (d *Detector) Start(ctx context.Context) {
	ticker := time.NewTicker(time.Duration(d.intervalSec) * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			connections := d.scan()
			for _, conn := range connections {
				d.collector.Record(metrics.Event{
					Type:        metrics.EventDirectConnection,
					ProcessName: conn.ProcessName,
					ProcessPID:  conn.PID,
					Provider:    conn.Provider,
					Action:      "detected",
				})

				d.events <- tui.Event{
					Type:        tui.EventDirectConn,
					Time:        time.Now(),
					ProcessName: conn.ProcessName,
					ProcessPID:  conn.PID,
					Provider:    conn.Provider,
				}
			}
		}
	}
}

func (d *Detector) scan() []DirectConnection {
	cmd := exec.Command("lsof", "-i", "-n", "-P")
	out, err := cmd.Output()
	if err != nil {
		log.Printf("lsof scan failed: %v", err)
		return nil
	}

	var connections []DirectConnection
	scanner := bufio.NewScanner(strings.NewReader(string(out)))
	for scanner.Scan() {
		line := scanner.Text()
		if !strings.Contains(line, "ESTABLISHED") {
			continue
		}
		for _, provider := range d.providers {
			if strings.Contains(line, provider) || d.matchesResolvedIP(line, provider) {
				conn := parseLsofLine(line, provider)
				if conn != nil {
					connections = append(connections, *conn)
				}
			}
		}
	}
	return connections
}

func (d *Detector) matchesResolvedIP(_ string, _ string) bool {
	return false
}

func parseLsofLine(line string, provider string) *DirectConnection {
	fields := strings.Fields(line)
	if len(fields) < 2 {
		return nil
	}
	pid, _ := strconv.Atoi(fields[1])
	return &DirectConnection{
		ProcessName: fields[0],
		PID:         pid,
		Provider:    fmt.Sprintf("direct connection to %s", provider),
	}
}
