# CRD Tester

[![Build Status Travis](https://travis-ci.org/ryandawsonuk/crdtester.svg?branch=master)](https://travis-ci.org/ryandawsonuk/crdtester)

This is a general-purpose tool for testing the use of a CRD. It checks for a CRD and then deploys a custom resource of that type and deletes it. It can optionally wait for the resource to match a spring expression condition before deleting (e.g. a ready status check). If it hits an issue (e.g. due to a faulty resource file) then it reports failure. 

The tool is parameterised to allow for different CRDs. See [Using Custom Configuration](#using-custom-configuration)

For deploy and delete commands by default the tool uses fabric8 kubernetes client but it can be set to delegate to shell actions to allow for other deployment methods (e.g. kubectl/helm).

## Pre-requisites

A cluster with a CRD installed should be accessible ([kube config file can be used](https://github.com/fabric8io/kubernetes-client/blob/master/README.md#configuring-the-client)). 

## How to Run with Default Configuration

To use the default configuration install the CRD from the initial steps in [this guide](https://github.com/SeldonIO/seldon-core/blob/master/notebooks/helm_examples.ipynb)

Clone this repository and run with `./mvnw spring-boot:run` (or `./mvnw.cmd spring-boot:run` on windows)

If successful then near the end of the output you will see logging including `deleted mymodel` and `watch closed`. The tool should fail if unsuccessful.

The custom resource contains a `status` and within that `state` which needs to be `Available` before deleting. The spring expression to match this is `unknownFields['status']['state'] eq 'Available'` - here `unknownFields` is a wrapper that allows custom resources to be handled in a general way without the check mechanism being tied to a particular CRD.

## Using Custom Configuration

Parameters can be overriden from application.properties file (in src/main/resources) or environment variables following spring boot convention ([relaxed binding](https://github.com/spring-projects/spring-boot/wiki/Relaxed-Binding-2.0)).

The tool expects a CRD of name `crd.name` in the cluster. It will attempt to deploy a resource file containing a resource of type `crd.name` and file to be specified in `deploy.resource.location`.

As an overriding example an application-helm.properties config file is provided. To use this run with `./mvnw spring-boot:run -Dspring.profiles.active=helm`

## Troubleshooting

A `ConnectException: Failed to connect` means no kubernetes cluster is configured or the cluster is unreachable. See fabric8 kubernetes client docs.

If delegating to shell commands and seeing resource created but not deleted, be careful about which namespace your kube context is pointed at.