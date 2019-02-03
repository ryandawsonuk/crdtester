# CRD Tester

This is a general-purpose tool for testing the use of a CRD. It checks for a CRD and then deploys a custom resource of that type and deletes it. It is parameterised to allow for different CRDs and can delegate to shell actions to allow for deployment methods (by default it uses fabric8 kubernetes client).

## Pre-requisites

Maven should be installed. 

A cluster with a CRD installed should be available. 

# How to Run with Default Configuration

To use the default configuration install the CRD from the initial steps in https://github.com/SeldonIO/seldon-core/blob/master/notebooks/helm_examples.ipynb

Then run with `mvn spring-boot:run`

If successful then near the end of the output you will see logging including `deleted mymodel` and `watch closed`

# Using Custom Configuration

Parameters can be overriden from application.properties or environment variables following spring boot convention (relaxed binding).

The tool expects a CRD of name `crd.name` in the cluster. It will attempt to deploy a resource file containing a resource of type `crd.name` and file to be specified in `deploy.resource.location`.

# Troubleshooting

A `ConnectException: Failed to connect` means no kubernetes cluster is configured or the cluster is unreachable. See fabric8 kubernetes client docs.