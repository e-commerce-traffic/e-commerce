-- replication 사용자 생성
CREATE USER IF NOT EXISTS 'replica_user'@'%' IDENTIFIED BY 'replica';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'replica_user'@'%';
FLUSH PRIVILEGES;

