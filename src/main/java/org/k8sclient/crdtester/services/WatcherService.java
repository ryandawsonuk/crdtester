package org.k8sclient.crdtester.services;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.Watchable;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.model.CustomResourceImplList;
import org.k8sclient.crdtester.model.DoneableCustomResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WatcherService {

    private static final Logger logger = LoggerFactory.getLogger(WatcherService.class);

    @Autowired
    private KubernetesClient kubernetesClient;

    @Autowired
    private DeleteService deleteService;

    @Value("${delete-resource}")
    private Boolean deleteResource;

    @Value("${timeout:5000}")
    private long timeout;

    @Value("${kubernetes.namespace}")
    private String namespace;

    public void waitForLatch(CountDownLatch deleteLatch, String message) {
        try {
            deleteLatch.await(timeout,
                              TimeUnit.MILLISECONDS);
        }catch (KubernetesClientException | InterruptedException e) {
            logger.error(message, e);
        }
    }


    public Watch createWatch(CustomResourceDefinition crd, String customResourceObjectName, boolean objectExistsAlready, CountDownLatch deleteLatch, CountDownLatch closeLatch) {
        Watchable watchable = kubernetesClient.customResources(crd, CustomResourceImpl.class, CustomResourceImplList.class, DoneableCustomResourceImpl.class).inNamespace(namespace).withResourceVersion("0");

        if(objectExistsAlready){
            //can just watch this obj rather than looking for it to be created
            watchable = kubernetesClient.customResources(crd, CustomResourceImpl.class, CustomResourceImplList.class, DoneableCustomResourceImpl.class).inNamespace(namespace).withName(customResourceObjectName);
        }

        Watch watch = (Watch)watchable.watch(new Watcher<CustomResourceImpl>() {
            @Override
            public void eventReceived(Action action, CustomResourceImpl resource) {
                logger.info("==> " + action + " for " + resource);

                if( action.equals(Action.ADDED)|| action.equals(Action.MODIFIED)){
                    if (deleteResource && deleteLatch.getCount()!=0 ) {

                        if(customResourceObjectName.equalsIgnoreCase(resource.getMetadata().getName())) {

                            deleteService.delete(crd,deleteLatch,resource);
                        } else {
                            logger.info("Not deleting object " + resource.getMetadata().getName());
                        }
                    }
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
                logger.info("watch closed");
                closeLatch.countDown();
            }
        });

        return watch;
    }



}
