package org.k8sclient.crdtester.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrdTesterProperties {


    @Value("${deploy.resource.location}")
    private String resourceLocation;

    @Value("${deploy.command}")
    private String deployCommand;

    @Value("${deploy.method}")
    private String deployMethod;

    public String getResourceLocation() {
        return resourceLocation;
    }

    public void setResourceLocation(String resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public String getDeployCommand() {
        return deployCommand;
    }

    public void setDeployCommand(String deployCommand) {
        this.deployCommand = deployCommand;
    }

    public String getDeployMethod() {
        return deployMethod;
    }

    public void setDeployMethod(String deployMethod) {
        this.deployMethod = deployMethod;
    }
}
