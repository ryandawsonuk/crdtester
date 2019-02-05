package org.k8sclient.crdtester.services.delete;

import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.services.KubernetesClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "delete.method",havingValue = "k8sclient",matchIfMissing = true)
public class Fabric8DeleteService implements DeleteService {

    @Value("${deploy.object.name}")
    private String objectName;

    private static final Logger logger = LoggerFactory.getLogger(Fabric8DeleteService.class);


    @Autowired
    private KubernetesClientService kubernetesClientService;

    @Override
    public void delete(CustomResourceDefinition crd,
                       CountDownLatch deleteLatch,
                       CustomResourceImpl resource) {

        boolean deleted = kubernetesClientService.createCrdClient(crd).delete(resource);
        if (deleted) {
            logger.info("deleted " + objectName);
            deleteLatch.countDown();
        } else {
            logger.error("delete via client failed");
        }
    }
}
