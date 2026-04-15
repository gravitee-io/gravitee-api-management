---
name: stop-apim
description: Gracefully shut down all Gravitee APIM components (Gateway, REST API, Console UI, MongoDB, Elasticsearch). Use when stopping development environment or when user mentions "stop apim", "shutdown apim", "kill apim".
---

# Stop APIM Stack

Gracefully shut down all locally running Gravitee APIM components.

## When to Use

Use this skill when:
- User wants to stop the APIM development environment
- Need to free up ports or resources
- Finished working with APIM for the day
- User mentions "stop", "shutdown", or "kill" APIM

## Stopping the Stack

This workflow uses port-based identification to ensure it works even if the original terminals are no longer accessible.

### Step 1: Stop Console UI (Port 4000)

```bash
# Try SIGTERM first, then SIGKILL if still running after 2s
PIDS=$(lsof -ti :4000)
if [ -n "$PIDS" ]; then
  kill $PIDS
  sleep 2
  STILL_RUNNING=$(lsof -ti :4000)
  if [ -n "$STILL_RUNNING" ]; then
    kill -9 $STILL_RUNNING 2>/dev/null || true
  fi
fi
echo "✅ Console UI stopped"
```

### Step 2: Stop REST API (Port 8083)

```bash
# Try SIGTERM first, then SIGKILL if still running after 2s
PIDS=$(lsof -ti :8083)
if [ -n "$PIDS" ]; then
  kill $PIDS
  sleep 2
  STILL_RUNNING=$(lsof -ti :8083)
  if [ -n "$STILL_RUNNING" ]; then
    kill -9 $STILL_RUNNING 2>/dev/null || true
  fi
fi
echo "✅ REST API stopped"
```

### Step 3: Stop Gateway (Port 8082)

```bash
# Try SIGTERM first, then SIGKILL if still running after 2s
PIDS=$(lsof -ti :8082)
if [ -n "$PIDS" ]; then
  kill $PIDS
  sleep 2
  STILL_RUNNING=$(lsof -ti :8082)
  if [ -n "$STILL_RUNNING" ]; then
    kill -9 $STILL_RUNNING 2>/dev/null || true
  fi
fi
echo "✅ Gateway stopped"
```

### Step 4: Stop Infrastructure (Docker)

```bash
docker stop gio_apim_mongodb gio_apim_elasticsearch 2>/dev/null || echo "Containers already stopped or not found"
```

### All-in-One Command

Stop everything with a single command:

```bash
# Stop Console UI
lsof -ti :4000 | xargs -r kill 2>/dev/null || true

# Stop REST API
lsof -ti :8083 | xargs -r kill 2>/dev/null || true

# Stop Gateway
lsof -ti :8082 | xargs -r kill 2>/dev/null || true

# Stop Docker containers
docker stop gio_apim_mongodb gio_apim_elasticsearch 2>/dev/null || true

echo "✅ All APIM services stopped"
```

## Verification

Verify all services are stopped:

```bash
# Check application ports
lsof -i :4000 -i :8082 -i :8083

# Check infrastructure ports
lsof -i :27017 -i :9200

# Check Docker containers
docker ps --filter "name=gio_apim"
```

Expected: No output for ports, and no running containers named `gio_apim_*`.

## Notes

- **Graceful shutdown**: This workflow tries SIGTERM first (graceful), then SIGKILL if needed
- **Port-based**: Works even if terminal sessions are lost
- **Docker containers**: Stopped but not removed (use `docker rm` to remove)
- **Data persistence**: MongoDB and Elasticsearch data persists in Docker volumes

## Restart After Stopping

To restart APIM after stopping, use `/start-apim` skill.
