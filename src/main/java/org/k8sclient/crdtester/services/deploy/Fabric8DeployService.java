package org.k8sclient.crdtester.services.deploy;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.k8sclient.crdtester.config.CrdTesterProperties;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.model.CustomResourceImplList;
import org.k8sclient.crdtester.model.DoneableCustomResourceImpl;
import org.k8sclient.crdtester.services.KubernetesClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "deploy.method",havingValue = "k8sclient",matchIfMissing = true)
public class Fabric8DeployService implements DeployService {

    private static final Logger logger = LoggerFactory.getLogger(Fabric8DeployService.class);

    @Autowired
    private CrdTesterProperties crdTesterProperties;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private KubernetesClientService kubernetesClientService;

    @Override
    public void deploy(CustomResourceDefinition crd) throws Exception {
        NonNamespaceOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, Resource<CustomResourceImpl, DoneableCustomResourceImpl>> crdClient = kubernetesClientService.createCrdClient(crd);

        CustomResourceImpl resource = crdClient.load(resourceLoader.getResource(crdTesterProperties.getResourceLocation()).getInputStream()).get();

        crdClient.createOrReplace(resource);
        logger.info("create or replace performed on "+resource);

    }
}
