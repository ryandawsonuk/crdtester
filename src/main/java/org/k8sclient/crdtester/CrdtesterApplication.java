package org.k8sclient.crdtester;

import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.services.KubernetesClientService;
import org.k8sclient.crdtester.services.WatcherService;
import org.k8sclient.crdtester.services.deploy.DeployService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class CrdtesterApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(CrdtesterApplication.class);

	@Autowired
	private KubernetesClient kubernetesClient;

	@Value("${crd.name}")
	private String crdName;

	@Value("${error-on-existing}")
	private Boolean errorOnExisting;

	@Value("${delete-resource}")
	private Boolean deleteResource;

	@Value("${kubernetes.namespace}")
	private String namespace;

	@Value("${deploy.object.name}")
	private String objectName;

	@Autowired
	private DeployService deployService;

	@Autowired
	private KubernetesClientService kubernetesClientService;

	@Autowired
	private WatcherService watcherService;


	public static void main(String[] args) {
		SpringApplication.run(CrdtesterApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {

		System.out.println(errorOnExisting);

		CustomResourceDefinition crd = getCRD();

		//have commented on https://github.com/fabric8io/fabric8-maven-plugin/issues/1377 as this approach could be used to fix that
		KubernetesDeserializer.registerCustomKind(crd.getSpec().getGroup() + "/"+crd.getSpec().getVersion(), crd.getSpec().getNames().getKind(), CustomResourceImpl.class);

		CustomResourceImpl deployedResource = getCustomResourceObject(crd);

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
		Watch watch = watcherService.createWatch(crd,objectName, deployedResource!=null, deleteLatch, closeLatch);


		deployService.deploy(crd);

		watcherService.waitForLatch(deleteLatch,"Failed to watch for and delete "+objectName);
		watch.close();
		watcherService.waitForLatch(closeLatch,"Failure in watch");
		kubernetesClient.close();

	}


	private CustomResourceImpl getCustomResourceObject(CustomResourceDefinition crd) {
		return kubernetesClientService.createCrdClient(crd).withName(objectName).get();
	}

	private CustomResourceDefinition getCRD() throws Exception {
		CustomResourceDefinition customResourceDefinition = kubernetesClient.customResourceDefinitions().withName(crdName).get();
		if(customResourceDefinition==null){
			throw new Exception("CRD "+crdName+ " not present");
		}
		return customResourceDefinition;
	}
}

