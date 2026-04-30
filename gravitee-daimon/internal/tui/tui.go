package tui

import (
	"context"
	"fmt"
	"strings"
	"time"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type EventType int

const (
	EventRequest EventType = iota
	EventBlocked
	EventDirectConn
	EventRegistration
	EventPolicyReload
)

type Event struct {
	Type        EventType
	Time        time.Time
	Model       string
	TokensIn    int
	TokenOut    int
	Latency     time.Duration
	Policy      string
	Reason      string
	Tool        string
	ProcessName string
	ProcessPID  int
	Provider    string
}

type model struct {
	ctx       context.Context
	events    <-chan Event
	logs      []string
	connected bool
	stats     stats
	width     int
	height    int
}

type stats struct {
	requests int
	blocked  int
	tokensIn int
	tokensOut int
	cost     float64
}

type tickMsg time.Time
type eventMsg Event

var (
	titleStyle = lipgloss.NewStyle().
			Bold(true).
			Foreground(lipgloss.Color("212"))

	successStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("42"))

	errorStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("196"))

	warnStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("214"))

	dimStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("241"))
)

func Run(ctx context.Context, events <-chan Event) {
	m := model{
		ctx:    ctx,
		events: events,
		logs:   make([]string, 0, 50),
	}

	p := tea.NewProgram(m, tea.WithAltScreen())
	p.Run()
}

func (m model) Init() tea.Cmd {
	return tea.Batch(tickCmd(), listenEvents(m.events))
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		if msg.String() == "q" || msg.String() == "ctrl+c" {
			return m, tea.Quit
		}

	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height

	case tickMsg:
		return m, tickCmd()

	case eventMsg:
		e := Event(msg)
		m.addLog(e)
		m.updateStats(e)
		return m, listenEvents(m.events)
	}

	return m, nil
}

func (m model) View() string {
	var b strings.Builder

	connStatus := errorStyle.Render("● Disconnected")
	if m.connected {
		connStatus = successStyle.Render("● Connected to Gateway")
	}

	header := titleStyle.Render("Gravitee DAImon v0.1.0") + "  " + connStatus
	b.WriteString(header + "\n\n")

	b.WriteString(titleStyle.Render("Traffic") + "\n")
	b.WriteString(strings.Repeat("─", min(60, m.width)) + "\n")

	maxLogs := min(len(m.logs), m.height-14)
	if maxLogs < 0 {
		maxLogs = 0
	}
	start := len(m.logs) - maxLogs
	if start < 0 {
		start = 0
	}
	for _, log := range m.logs[start:] {
		b.WriteString(log + "\n")
	}
	if len(m.logs) == 0 {
		b.WriteString(dimStyle.Render("  Waiting for traffic...") + "\n")
	}

	b.WriteString("\n")
	b.WriteString(titleStyle.Render("Metrics") + "\n")
	b.WriteString(strings.Repeat("─", 30) + "\n")
	b.WriteString(fmt.Sprintf("  Requests:  %d\n", m.stats.requests))
	b.WriteString(fmt.Sprintf("  Blocked:   %d\n", m.stats.blocked))
	b.WriteString(fmt.Sprintf("  Tokens in: %s\n", formatNumber(m.stats.tokensIn)))
	b.WriteString(fmt.Sprintf("  Tokens out:%s\n", formatNumber(m.stats.tokensOut)))

	b.WriteString("\n" + dimStyle.Render("[q] quit") + "\n")

	return b.String()
}

func (m *model) addLog(e Event) {
	ts := e.Time.Format("15:04:05")
	var line string
	switch e.Type {
	case EventRequest:
		line = fmt.Sprintf("  %s  %s %s  %s→%s tok  %s",
			ts,
			successStyle.Render("✓"),
			e.Model,
			formatNumber(e.TokensIn),
			formatNumber(e.TokenOut),
			dimStyle.Render(e.Latency.String()),
		)
	case EventBlocked:
		line = fmt.Sprintf("  %s  %s %s  BLOCKED: %s",
			ts,
			errorStyle.Render("✗"),
			e.Model,
			e.Reason,
		)
	case EventDirectConn:
		line = fmt.Sprintf("  %s  %s DIRECT: %s (PID %d) → %s",
			ts,
			warnStyle.Render("⚠"),
			e.ProcessName,
			e.ProcessPID,
			e.Provider,
		)
	case EventRegistration:
		line = fmt.Sprintf("  %s  %s %s",
			ts,
			dimStyle.Render("⟳"),
			e.Reason,
		)
		if strings.Contains(e.Reason, "successfully") {
			m.connected = true
		}
	case EventPolicyReload:
		line = fmt.Sprintf("  %s  %s policies reloaded",
			ts,
			dimStyle.Render("⟳"),
		)
	}

	if len(m.logs) >= 50 {
		m.logs = m.logs[1:]
	}
	m.logs = append(m.logs, line)
}

func (m *model) updateStats(e Event) {
	switch e.Type {
	case EventRequest:
		m.stats.requests++
		m.stats.tokensIn += e.TokensIn
		m.stats.tokensOut += e.TokenOut
	case EventBlocked:
		m.stats.requests++
		m.stats.blocked++
	}
}

func tickCmd() tea.Cmd {
	return tea.Tick(time.Second, func(t time.Time) tea.Msg {
		return tickMsg(t)
	})
}

func listenEvents(ch <-chan Event) tea.Cmd {
	return func() tea.Msg {
		e, ok := <-ch
		if !ok {
			return tea.Quit()
		}
		return eventMsg(e)
	}
}

func formatNumber(n int) string {
	if n >= 1000 {
		return fmt.Sprintf("%d,%03d", n/1000, n%1000)
	}
	return fmt.Sprintf("%d", n)
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
