package org.k8sclient.crdtester.services.delete;

import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.services.ShellCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "delete.method",havingValue = "command")
public class ShellDeleteService implements DeleteService {

    @Value("${delete.command}")
    private String deleteCommand;

    @Value("${deploy.object.name}")
    private String objectName;

    @Autowired
    private ShellCommandService shellCommandService;

    private static final Logger logger = LoggerFactory.getLogger(ShellDeleteService.class);

    @Override
    public void delete(CustomResourceDefinition crd,
                       CountDownLatch deleteLatch,
                       CustomResourceImpl resource) {

        try {
            deleteResourceUsingShellCommand(crd.getMetadata().getName());
            logger.info("deleted " + objectName);
            deleteLatch.countDown();
        } catch (Exception e) {
            logger.error("deletion via command failed",
                         e);
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
