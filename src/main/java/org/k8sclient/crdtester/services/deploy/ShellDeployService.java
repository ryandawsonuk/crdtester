package org.k8sclient.crdtester.services.deploy;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import org.k8sclient.crdtester.config.CrdTesterProperties;
import org.k8sclient.crdtester.services.ShellCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "deploy.method",havingValue = "command")
public class ShellDeployService implements DeployService {

    private static final Logger logger = LoggerFactory.getLogger(ShellDeployService.class);

    @Autowired
    private CrdTesterProperties crdTesterProperties;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ShellCommandService shellCommandService;

    public void deploy(CustomResourceDefinition crd) throws Exception{
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
