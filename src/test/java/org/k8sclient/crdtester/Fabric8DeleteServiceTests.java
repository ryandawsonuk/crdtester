package org.k8sclient.crdtester;

import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.model.CustomResourceImplList;
import org.k8sclient.crdtester.model.DoneableCustomResourceImpl;
import org.k8sclient.crdtester.services.KubernetesClientService;
import org.k8sclient.crdtester.services.delete.Fabric8DeleteService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class Fabric8DeleteServiceTests {

	@InjectMocks
	@Spy
	private Fabric8DeleteService deleteService;

	@Mock
	private KubernetesClientService kubernetesClientService;

	@Mock
	private NonNamespaceOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, Resource<CustomResourceImpl, DoneableCustomResourceImpl>> crdClient;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
	}

	@Test
	public void deletesWithClient() throws Exception {

		CustomResourceImpl customResource = new CustomResourceImpl();

		when(kubernetesClientService.createCrdClient(any())).thenReturn(crdClient);
		when(crdClient.delete(customResource)).thenReturn(true);

		deleteService.delete(null,new CountDownLatch(1),customResource);
		verify(kubernetesClientService).createCrdClient(any());
	}


}

