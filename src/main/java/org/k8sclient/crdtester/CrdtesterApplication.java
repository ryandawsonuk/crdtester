package org.k8sclient.crdtester;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class CrdtesterApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(CrdtesterApplication.class);

	@Autowired
	private KubernetesClient kubernetesClient;

	@Value("${crd.name}")
	private String crdName;

	@Value("${deploy.resource.location}")
	private String resourceLocation;

	@Value("${deploy.object.name}")
	private String objectName;

	@Value("${deploy.command}")
	private String deployCommand;

	@Value("${deploy.method}")
	private String deployMethod;

	@Value("${error.on.existing}")
	private Boolean errorOnExisting;

	@Value("${kubernetes.namespace}")
	private String namespace;

	@Autowired
	private ResourceLoader resourceLoader;

	public static void main(String[] args) {
		SpringApplication.run(CrdtesterApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {

		CustomResourceDefinition crd = getCRD();

		//seems fabric8 needs custom type to handle crds - have commented on https://github.com/fabric8io/fabric8-maven-plugin/issues/1377
		KubernetesDeserializer.registerCustomKind(crd.getSpec().getGroup() + "/"+crd.getSpec().getVersion(), crd.getSpec().getNames().getKind(), CustomResourceImpl.class);

		CustomResource deployedResource = getCustomResourceObject(crd);
		if(deployedResource!=null && errorOnExisting){
			throw new Exception("Object "+crdName+"/"+objectName+" already exists");
		} else{
			logger.info("Object "+crdName+"/"+objectName+" already exists");
		}

		createWatch(crd);

		if(deployMethod.equalsIgnoreCase("command")){
			loadResourceUsingShellCommand();
		} else{
			loadResourceFromFile(crd);
		}

	}

	private void loadResourceUsingShellCommand() throws Exception{

		if(deployCommand==null || deployCommand.equalsIgnoreCase("")){
			deployCommand = "kubectl create -f "+resourceLoader.getResource(resourceLocation).getFile().getAbsoluteFile();
		}

		logger.info("deploying with "+deployCommand);

		ProcessBuilder pb = new ProcessBuilder(deployCommand.split(" "));

		Process process = pb.start();
		StreamGobbler streamGobbler =
				new StreamGobbler(process.getInputStream(), System.out::println);
		Executors.newSingleThreadExecutor().submit(streamGobbler);
		int exitCode = process.waitFor();
		if(exitCode != 0){
			throw new Exception("Failure on running: "+deployCommand);
		}
	}


	private CustomResourceImpl loadResourceFromFile(CustomResourceDefinition crd) throws Exception {

		File file = resourceLoader.getResource(resourceLocation).getFile();

		NonNamespaceOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, Resource<CustomResourceImpl, DoneableCustomResourceImpl>> crdClient = createCrdClient(crd);

		CustomResourceImpl resource = crdClient.load(new FileInputStream(file)).get();

		crdClient.createOrReplace(resource);
		logger.info("create or replace performed on "+resource);

		return resource;
	}

	private NonNamespaceOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, Resource<CustomResourceImpl, DoneableCustomResourceImpl>> createCrdClient(CustomResourceDefinition crd) {
		NonNamespaceOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, Resource<CustomResourceImpl, DoneableCustomResourceImpl>> crdClient = kubernetesClient.customResources(crd, CustomResourceImpl.class, CustomResourceImplList.class, DoneableCustomResourceImpl.class);

		if(namespace!=null && !namespace.equalsIgnoreCase("")){
			crdClient = ((MixedOperation<CustomResourceImpl, CustomResourceImplList, DoneableCustomResourceImpl, Resource<CustomResourceImpl, DoneableCustomResourceImpl>>) crdClient).inNamespace(namespace);
		}
		return crdClient;
	}

	private CustomResource getCustomResourceObject(CustomResourceDefinition crd) {
		return createCrdClient(crd).withName(objectName).get();
	}

	private void createWatch(CustomResourceDefinition crd) {
		kubernetesClient.customResources(crd, CustomResourceImpl.class, CustomResourceImplList.class, DoneableCustomResourceImpl.class).inNamespace(namespace).withName(objectName).watch(new Watcher<CustomResourceImpl>() {
			@Override
			public void eventReceived(Action action, CustomResourceImpl resource) {
				logger.info("==> " + action + " for " + resource);
				if (resource.getSpec() == null) {
					logger.error("No Spec for resource " + resource);
				}
			}

			@Override
			public void onClose(KubernetesClientException cause) {
			}
		});


	}


	private CustomResourceDefinition getCRD() throws Exception {
		CustomResourceDefinition customResourceDefinition = kubernetesClient.customResourceDefinitions().withName(crdName).get();
		if(customResourceDefinition==null){
			throw new Exception("CRD "+crdName+ " not present");
		}
		return customResourceDefinition;
	}
}

