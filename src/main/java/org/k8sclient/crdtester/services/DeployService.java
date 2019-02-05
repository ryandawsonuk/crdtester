package org.k8sclient.crdtester.services;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.k8sclient.crdtester.config.CrdTesterProperties;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.model.CustomResourceImplList;
import org.k8sclient.crdtester.model.DoneableCustomResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class DeployService {

    private static final Logger logger = LoggerFactory.getLogger(DeployService.class);

    @Autowired
    private CrdTesterProperties crdTesterProperties;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ShellCommandService shellCommandService;

    @Autowired
    private KubernetesClientService kubernetesClientService;

    public void deploy(CustomResourceDefinition crd) throws Exception{
        if(crdTesterProperties.getDeployMethod().equalsIgnoreCase("command")){
            loadResourceUsingShellCommand();
        } else{
            loadResourceFromFileUsingClient(crd);
        }
    }

    public CustomResourceImpl loadResourceFromFileUsingClient(CustomResourceDefinition crd) throws Exception {

        NonNamespaceOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, Resource<CustomResourceImpl, DoneableCustomResourceImpl>> crdClient = kubernetesClientService.createCrdClient(crd);

        CustomResourceImpl resource = crdClient.load(resourceLoader.getResource(crdTesterProperties.getResourceLocation()).getInputStream()).get();

        crdClient.createOrReplace(resource);
        logger.info("create or replace performed on "+resource);

        return resource;
    }

    public void loadResourceUsingShellCommand() throws Exception{

        String deployCommand = crdTesterProperties.getDeployCommand();
        if(crdTesterProperties.getDeployCommand()==null || crdTesterProperties.getDeployCommand().equalsIgnoreCase("")){
            if(resourceLoader.getResource(crdTesterProperties.getResourceLocation()).isFile()) {
                deployCommand = "kubectl create -f " +resourceLoader.getResource(crdTesterProperties.getResourceLocation()).getFile().getAbsoluteFile();
            } else{
                deployCommand = "kubectl create -f " +resourceLoader.getResource(crdTesterProperties.getResourceLocation()).getURL();
            }
        }

        logger.info("deploying with "+deployCommand);

        shellCommandService.executeShellCommand(deployCommand);
    }


}
