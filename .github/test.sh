#!/bin/bash

# Build Docker

docker network create codemc

docker build -t codemc/mariadb src/test/docker/mariadb
docker run -d -p 3306:3306 --name mariadb --network codemc codemc/mariadb

docker build -t codemc/jenkins src/test/docker/jenkins
docker run -d -p 8080:8080 --name jenkins-rest --network codemc codemc/jenkins

docker build -t codemc/nexus src/test/docker/nexus
docker run -d -p 8081:8081 --name nexus-rest --network codemc codemc/nexus

# Wait for Services

echo "Waiting for MariaDB to be ready..."
until docker exec mariadb mariadb -u root -e "SELECT 1"; do
  sleep 2
done

echo "Waiting for Jenkins to be ready..."
until curl -s "http://localhost:8080" | grep "Dashboard"; do
  sleep 2
done

echo "Waiting for Nexus to be ready..."
until curl -s "http://localhost:8081"; do
  sleep 2
done

# Copy Nexus Password
docker cp nexus-rest:/nexus-data/admin.password /tmp/admin.password