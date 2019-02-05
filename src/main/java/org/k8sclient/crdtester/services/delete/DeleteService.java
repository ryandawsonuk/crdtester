package org.k8sclient.crdtester.services.delete;

import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import org.k8sclient.crdtester.model.CustomResourceImpl;

public interface DeleteService {

    public void delete(CustomResourceDefinition crd, CountDownLatch deleteLatch, CustomResourceImpl resource);
}
