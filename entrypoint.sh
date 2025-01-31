#!/bin/bash
set -e

# Start Redis server
redis-server --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --port 6379 &

# Wait for Redis to start
sleep 5

# Initialize Redis Cluster
redis-cli --cluster create \
  redis-node1:6379 \
  redis-node2:6380 \
  redis-node3:6381 \
  --cluster-replicas 1

# Keep container running
tail -f /dev/null
