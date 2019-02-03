package org.k8sclient.crdtester.services;

import java.io.File;
import java.io.FileInputStream;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
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

    @Value("${deploy.resource.location}")
    private String resourceLocation;

    @Value("${deploy.command}")
    private String deployCommand;

    @Value("${deploy.method}")
    private String deployMethod;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ShellCommandService shellCommandService;

    @Autowired
    private KubernetesClientService kubernetesClientService;

    public void deploy(CustomResourceDefinition crd) throws Exception{
        if(deployMethod.equalsIgnoreCase("command")){
            loadResourceUsingShellCommand();
        } else{
            loadResourceFromFileUsingClient(crd);
        }
    }

    private CustomResourceImpl loadResourceFromFileUsingClient(CustomResourceDefinition crd) throws Exception {

        File file = resourceLoader.getResource(resourceLocation).getFile();

        NonNamespaceOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, Resource<CustomResourceImpl, DoneableCustomResourceImpl>> crdClient = kubernetesClientService.createCrdClient(crd);

        CustomResourceImpl resource = crdClient.load(new FileInputStream(file)).get();

        crdClient.createOrReplace(resource);
        logger.info("create or replace performed on "+resource);

        return resource;
    }

    private void loadResourceUsingShellCommand() throws Exception{

        if(deployCommand==null || deployCommand.equalsIgnoreCase("")){
            deployCommand = "kubectl create -f "+resourceLoader.getResource(resourceLocation).getFile().getAbsoluteFile();
        }

        logger.info("deploying with "+deployCommand);

        shellCommandService.executeShellCommand(deployCommand);
    }


}
