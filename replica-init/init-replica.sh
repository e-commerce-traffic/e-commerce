#!/bin/bash
set -e
set -x

MASTER_HOST=db-master
MASTER_USER=replica_user
MASTER_PASSWORD=replica
MYSQL_ROOT_PASSWORD=mysql

echo "Fetching master status..."

# 마스터 상태 가져오기 (DB 지정 없음)
MASTER_STATUS=$(mysql -h $MASTER_HOST -u $MASTER_USER -p"$MASTER_PASSWORD" -N -e "SHOW MASTER STATUS;")
MASTER_LOG_FILE=$(echo "$MASTER_STATUS" | awk 'NR==1 {print $1}')
MASTER_LOG_POS=$(echo "$MASTER_STATUS" | awk 'NR==1 {print $2}')

if [ -z "$MASTER_LOG_FILE" ] || [ -z "$MASTER_LOG_POS" ]; then
  echo "Master log file or position is empty."
  exit 1
fi

echo "Master log file: $MASTER_LOG_FILE, Master log position: $MASTER_LOG_POS"

echo "Stopping and resetting slave..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "STOP SLAVE; RESET SLAVE ALL;"

echo "Configuring replication..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "
CHANGE MASTER TO
  MASTER_HOST='$MASTER_HOST',
  MASTER_USER='$MASTER_USER',
  MASTER_PASSWORD='$MASTER_PASSWORD',
  MASTER_LOG_FILE='$MASTER_LOG_FILE',
  MASTER_LOG_POS=$MASTER_LOG_POS;
START SLAVE;
"

echo "Replication setup complete."

