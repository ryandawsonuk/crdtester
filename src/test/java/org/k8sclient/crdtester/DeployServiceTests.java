package org.k8sclient.crdtester;


import java.io.InputStream;
import java.net.URL;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.k8sclient.crdtester.config.CrdTesterProperties;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.model.CustomResourceImplList;
import org.k8sclient.crdtester.model.DoneableCustomResourceImpl;
import org.k8sclient.crdtester.services.DeployService;
import org.k8sclient.crdtester.services.KubernetesClientService;
import org.k8sclient.crdtester.services.ShellCommandService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


@RunWith(MockitoJUnitRunner.class)
public class DeployServiceTests {

	@InjectMocks
	@Spy
	private DeployService deployService;

	@Mock
	private CrdTesterProperties crdTesterProperties;

	@Mock
	private ResourceLoader resourceLoader;

	@Mock
	private Resource resource;

	@Mock
	private InputStream inputStream;

	@Mock
	private ShellCommandService shellCommandService;

	@Mock
	private io.fabric8.kubernetes.client.dsl.Resource customResource;

	@Mock
	private KubernetesClientService kubernetesClientService;

	@Mock
	private NonNamespaceOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, io.fabric8.kubernetes.client.dsl.Resource<CustomResourceImpl, DoneableCustomResourceImpl>> crdClient;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
	}

	@Test
	public void deploysWithClient() throws Exception {
		when(crdTesterProperties.getDeployMethod()).thenReturn("k8sclient");
		when(resourceLoader.getResource(any())).thenReturn(resource);
		when(resource.getInputStream()).thenReturn(inputStream);
		when(kubernetesClientService.createCrdClient(any())).thenReturn(crdClient);
		when(crdClient.load(inputStream)).thenReturn(customResource);
		deployService.deploy(new CustomResourceDefinition());
		verify(kubernetesClientService).createCrdClient(any());
	}

	@Test
	public void deploysWithShell() throws Exception {
		when(crdTesterProperties.getDeployMethod()).thenReturn("command");
		when(resourceLoader.getResource(any())).thenReturn(resource);
		when(resource.isFile()).thenReturn(false);
		when(resource.getURL()).thenReturn(new URL("http://fakeurl"));
		deployService.deploy(new CustomResourceDefinition());
		verify(shellCommandService).executeShellCommand(any());
	}

}

