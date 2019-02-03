package org.k8sclient.crdtester;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.Watchable;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.model.CustomResourceImplList;
import org.k8sclient.crdtester.model.DoneableCustomResourceImpl;
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

	@Value("${delete.command}")
	private String deleteCommand;

	@Value("${delete.method}")
	private String deleteMethod;

	@Value("${error-on-existing}")
	private Boolean errorOnExisting;

	@Value("${delete-resource}")
	private Boolean deleteResource;

	@Value("${kubernetes.namespace}")
	private String namespace;

	@Value("${timeout:5000}")
	private long timeout;

	@Autowired
	private ResourceLoader resourceLoader;

	public static void main(String[] args) {
		SpringApplication.run(CrdtesterApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {

		CustomResourceDefinition crd = getCRD();

		//have commented on https://github.com/fabric8io/fabric8-maven-plugin/issues/1377 as this approach could be used to fix that
		KubernetesDeserializer.registerCustomKind(crd.getSpec().getGroup() + "/"+crd.getSpec().getVersion(), crd.getSpec().getNames().getKind(), CustomResourceImpl.class);

		CustomResource deployedResource = getCustomResourceObject(crd);
		if(deployedResource!=null){
			if(errorOnExisting) {
				throw new Exception("Object " + crdName + "/" + objectName + " already exists");
			}
			logger.info("Object "+crdName+"/"+objectName+" already exists in namespace "+namespace);
		} else{
			logger.info("Object to be created is "+crdName+"/"+objectName+" in namespace "+namespace);
		}

		CountDownLatch deleteLatch = new CountDownLatch(deleteResource ? 1 : 0);
		CountDownLatch closeLatch = new CountDownLatch(1);
		Watch watch = createWatch(crd,objectName, deployedResource!=null, deleteLatch, closeLatch);


		if(deployMethod.equalsIgnoreCase("command")){
			loadResourceUsingShellCommand();
		} else{
			loadResourceFromFileUsingClient(crd);
		}

		waitForLatch(deleteLatch,"Failed to watch for and delete "+objectName);
		watch.close();
		waitForLatch(closeLatch,"Failure in watch");
		kubernetesClient.close();

	}

	private void waitForLatch(CountDownLatch deleteLatch, String message) {
		try {
			deleteLatch.await(timeout,
							  TimeUnit.MILLISECONDS);
		}catch (KubernetesClientException | InterruptedException e) {
			logger.error(message, e);
		}
	}

	private void loadResourceUsingShellCommand() throws Exception{

		if(deployCommand==null || deployCommand.equalsIgnoreCase("")){
			deployCommand = "kubectl create -f "+resourceLoader.getResource(resourceLocation).getFile().getAbsoluteFile();
		}

		logger.info("deploying with "+deployCommand);

		executeShellCommand(deployCommand);
	}

	private void deleteResourceUsingShellCommand() throws Exception{

		if(deleteCommand==null || deleteCommand.equalsIgnoreCase("")){
			deleteCommand = "kubectl delete "+crdName+" "+objectName;
		}

		logger.info("deleting with "+deleteCommand);

		executeShellCommand(deleteCommand);
	}

	private void executeShellCommand(String command) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));

		Process process = pb.inheritIO().start();
		int exitCode = process.waitFor();
		if(exitCode != 0){
			throw new Exception("Failure on running: "+command);
		}
	}

	private CustomResourceImpl loadResourceFromFileUsingClient(CustomResourceDefinition crd) throws Exception {

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

	private Watch createWatch(CustomResourceDefinition crd, String customResourceObjectName, boolean objectExistsAlready, CountDownLatch deleteLatch, CountDownLatch closeLatch) {
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

							if (deployMethod.equalsIgnoreCase("command")) {
								try {
									deleteResourceUsingShellCommand();
									logger.info("deleted " + customResourceObjectName);
									deleteLatch.countDown();
								} catch (Exception e) {
									logger.error("deletion via command failed",
												 e);
								}
							} else {
								boolean deleted = createCrdClient(crd).delete(resource);
								if (deleted) {
									logger.info("deleted " + customResourceObjectName);
									deleteLatch.countDown();
								} else {
									logger.error("delete via client failed");
								}
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


	private CustomResourceDefinition getCRD() throws Exception {
		CustomResourceDefinition customResourceDefinition = kubernetesClient.customResourceDefinitions().withName(crdName).get();
		if(customResourceDefinition==null){
			throw new Exception("CRD "+crdName+ " not present");
		}
		return customResourceDefinition;
	}
}

