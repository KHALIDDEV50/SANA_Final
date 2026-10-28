#!/usr/bin/env bash
# Updates the live app: gets the latest code from GitHub, rebuilds and restarts.
# Run on the EC2 server, inside the SANA folder: ./deploy.sh
set -e
cd "$(dirname "$0")"

git pull
docker compose up -d --build
docker image prune -f

echo "Deployed. To watch the logs: docker compose logs -f app"
