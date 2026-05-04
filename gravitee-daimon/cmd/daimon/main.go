package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/detector"
	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/metrics"
	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/policy"
	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/proxy"
	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/registration"
	"github.com/gravitee-io/gravitee-api-management/gravitee-daimon/internal/tui"
)

func main() {
	asHost := flag.String("as", "", "simulate a different hostname (for demo)")
	flag.Parse()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGINT, syscall.SIGTERM)

	cfg, err := LoadConfig("config.yaml")
	if err != nil {
		log.Fatalf("failed to load config: %v", err)
	}

	hostname := *asHost
	if hostname == "" {
		hostname, _ = os.Hostname()
	}

	collector := metrics.NewCollector(hostname, cfg.Metrics.BaseDir)
	defer collector.Close()

	engine, err := policy.NewEngine("policies.yaml")
	if err != nil {
		log.Fatalf("failed to load policies: %v", err)
	}
	defer engine.Close()

	events := make(chan tui.Event, 100)

	reg := registration.NewClient(cfg.Gateway.ManagementURL, cfg.Registration.HeartbeatIntervalSec, cfg.Gateway.ManagementUsername, cfg.Gateway.ManagementPassword)
	go reg.Start(ctx, events)

	det := detector.New(cfg.Detector.Providers, cfg.Detector.ScanIntervalSec, collector, events)
	go det.Start(ctx)

	var sessionTag string
	if *asHost != "" {
		sessionTag = "as " + *asHost + " daimon"
	}
	p := proxy.New(cfg.Gateway.URL+cfg.Gateway.AIAPIPath, sessionTag, engine, collector, events)
	go p.Start(cfg.Proxy.ListenPort)

	fmt.Printf("DAImon listening on :%d → %s\n", cfg.Proxy.ListenPort, cfg.Gateway.URL)

	go func() {
		<-sig
		cancel()
	}()

	tui.Run(ctx, events)
}
