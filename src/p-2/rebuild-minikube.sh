#!/bin/bash
set -e

echo "==== Destroying existing Minikube ===="
minikube stop
minikube delete

echo "==== Creating fresh Minikube instance ===="
minikube start --driver=docker --cpus=4 --memory=4096

echo "==== Setting up Docker environment for Minikube ===="
eval $(minikube docker-env)

echo "==== Building Docker images ===="
docker build -t eureka-server:latest -f ../p-1/microservices/service-discovery/Dockerfile ../p-1/microservices/service-discovery
docker build -t api-gateway:latest -f ../p-1/microservices/api-gateway/Dockerfile ../p-1/microservices/api-gateway
docker build -t user-microservice:latest -f ../p-1/microservices/user-microservice/Dockerfile ../p-1/microservices/user-microservice
docker build -t ebike-microservice:latest -f ../p-1/microservices/ebike-microservice/Dockerfile ../p-1/microservices/ebike-microservice
docker build -t map-microservice:latest -f ../p-1/microservices/map-microservice/Dockerfile ../p-1/microservices/map-microservice
docker build -t ride-microservice:latest -f ../p-1/microservices/ride-microservice/Dockerfile ../p-1/microservices/ride-microservice

echo "==== Deploying to Kubernetes ===="
echo "Creating ConfigMap..."
kubectl create ns sap-assignment
kubectl apply -f ./k8s/configMap.yaml
echo "Deploying infrastructure..."
kubectl apply -f ./k8s/kafka/zookeeper.yaml
kubectl apply -f ./k8s/kafka/kafka-broker-1.yaml
kubectl apply -f ./k8s/kafka/schema-registry.yaml
kubectl apply -f ./k8s/mongodb.yaml
kubectl apply -f ./k8s/prometheus.yaml
kubectl apply -f ./k8s/api-gateway.yaml
kubectl apply -f ./k8s/eureka-server.yml

echo "Deploying services..."
kubectl apply -f ./k8s/user-microservice.yaml
kubectl apply -f ./k8s/ebike-microservice.yaml
kubectl apply -f ./k8s/ride-microservice.yaml
kubectl apply -f ./k8s/map-microservice.yaml

echo "Deploying Kafka UI"
kubectl apply -f ./k8s/kafka/kafka-ui.yaml

echo "==== Minikube IP address ===="
minikube ip

echo "==== Deployment complete. Checking status... ===="
watch kubectl get pods -n sap-assignment