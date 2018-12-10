#!/usr/bin/env bash
docker build -t localhost:5000/propy-transaction-service:latest ./
docker push localhost:5000/propy-transaction-service:latest
