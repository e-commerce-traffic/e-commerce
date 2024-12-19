#!/bin/bash
set -e
set -x

MYSQL_ROOT_PASSWORD=mysql

echo "Starting MySQL server in the background..."
docker-entrypoint.sh mysqld &

# MySQL 서버가 준비될 때까지 대기
until mysqladmin ping -h 127.0.0.1 -u root -p"$MYSQL_ROOT_PASSWORD" --protocol=TCP --silent; do
    echo "Waiting for MySQL server to be ready..."
    sleep 2
done

echo "MySQL server is up. Starting replication setup..."

## 레플리카 초기화 스크립트 실행
/replica-init/init-replica.sh
#
## MySQL 서버를 포그라운드로 가져오기
#fg %1

tail -f /dev/null