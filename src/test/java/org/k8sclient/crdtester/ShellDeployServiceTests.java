package org.k8sclient.crdtester;

import java.io.InputStream;
import java.net.URL;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.k8sclient.crdtester.config.CrdTesterProperties;
import org.k8sclient.crdtester.services.ShellCommandService;
import org.k8sclient.crdtester.services.deploy.ShellDeployService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ShellDeployServiceTests {


    @InjectMocks
    @Spy
    private ShellDeployService deployService;

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
