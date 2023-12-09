#!/bin/bash -e

helm repo add graviteeio https://helm.gravitee.io

helm repo update

helm install graviteeio-apim3x graviteeio/apim3 --create-namespace --namespace gravitee-apim

