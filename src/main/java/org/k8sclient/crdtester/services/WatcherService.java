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
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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

    @Value("${timeout:50000}")
    private long timeout;

    @Value("${kubernetes.namespace}")
    private String namespace;

    @Value("${predelete-condition}")
    private String predeleteCondition;

    public void waitForLatch(CountDownLatch deleteLatch, String message) {
        try {
            deleteLatch.await(timeout,
                              TimeUnit.MILLISECONDS);
        }catch (KubernetesClientException | InterruptedException e) {
            logger.error(message, e);
        }
    }

    public boolean checkExpression(CustomResourceImpl deployedResource, String predeleteCondition){
        String predicate = predeleteCondition;
        StandardEvaluationContext context = new StandardEvaluationContext();

        context.setRootObject(deployedResource);
        ExpressionParser expressionParser = new SpelExpressionParser();

        Expression expression = expressionParser.parseExpression(predicate);
        try {
            boolean result = expression.getValue(context,
                                                   Boolean.class);
            logger.info(result+" for "+predeleteCondition);
            return result;

        }catch (SpelEvaluationException ex){
            logger.warn("Predelete expression failed "+predeleteCondition);
        }
        return false;
    }


    public Watch createWatch(CustomResourceDefinition crd, String customResourceObjectName, boolean objectExistsAlready, CountDownLatch deleteLatch, CountDownLatch closeLatch) throws InterruptedException {
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

                            if(predeleteCondition==null || predeleteCondition.equalsIgnoreCase("") || checkExpression(resource,predeleteCondition)) {
                                deleteService.delete(crd,
                                                     deleteLatch,
                                                     resource);
                            }
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
