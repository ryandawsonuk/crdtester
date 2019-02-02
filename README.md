# CRD Tester

First have installed a CRD e.g. follow https://github.com/SeldonIO/seldon-core/blob/master/notebooks/helm_examples.ipynb

Run with `mvn spring-boot:run`

Parameters from application.properties or environment variables following spring boot convention (relaxed binding).

Expects a CRD of name `crd.name` in the cluster.

Will attempt to deploy a resource file containing a resource of type `crd.name` and file to be specified in `deploy.resource.location`.

A `ConnectException: Failed to connect` means no kubernetes cluster is configured or the cluster is unreachable. See fabric8 kubernetes client docs.