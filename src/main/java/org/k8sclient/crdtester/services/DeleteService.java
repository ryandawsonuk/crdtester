package org.k8sclient.crdtester.services;

import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeleteService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteService.class);

    @Value("${delete.command}")
    private String deleteCommand;

    @Value("${delete.method}")
    private String deleteMethod;

    @Value("${deploy.object.name}")
    private String objectName;

    @Autowired
    private KubernetesClientService kubernetesClientService;

    @Autowired
    private ShellCommandService shellCommandService;

    public void delete(CustomResourceDefinition crd, CountDownLatch deleteLatch, CustomResourceImpl resource){
        if (deleteMethod.equalsIgnoreCase("command")) {
            try {
                deleteResourceUsingShellCommand(crd.getMetadata().getName());
                logger.info("deleted " + objectName);
                deleteLatch.countDown();
            } catch (Exception e) {
                logger.error("deletion via command failed",
                             e);
            }
        } else {
            boolean deleted = kubernetesClientService.createCrdClient(crd).delete(resource);
            if (deleted) {
                logger.info("deleted " + objectName);
                deleteLatch.countDown();
            } else {
                logger.error("delete via client failed");
            }
        }
    }

    public void deleteResourceUsingShellCommand(String crdName) throws Exception{

        if(deleteCommand==null || deleteCommand.equalsIgnoreCase("")){
            deleteCommand = "kubectl delete "+crdName+" "+objectName;
        }

        logger.info("deleting with "+deleteCommand);

        shellCommandService.executeShellCommand(deleteCommand);
    }



}
