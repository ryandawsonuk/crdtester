package org.k8sclient.crdtester.services.deploy;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;

public interface DeployService {

    public void deploy(CustomResourceDefinition crd) throws Exception;

}
