package org.k8sclient.crdtester;

import java.io.FileInputStream;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrdtesterApplication implements CommandLineRunner {

	@Autowired
	private KubernetesClient kubernetesClient;

	@Value("${crd.name}")
	private String crdName;

	@Value("${resource.location}")
	private String resourceLocation;

	public static void main(String[] args) {
		SpringApplication.run(CrdtesterApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {

		//TODO: use a proper logger
		System.out.println("running here!");

		CustomResourceDefinition crd = getCRD();

		//check if any resource of this type is present?
		//need to look at
		CustomResourceList resourceList = (CustomResourceList)kubernetesClient.customResources(crd, CustomResource.class, CustomResourceList.class, CustomResourceDoneable.class).list();
		if(!resourceList.getItems().isEmpty()){
			System.out.println("already resources present! will they get deleted? Are we loading resource from file?");
		}

		//TODO: should check file contains resource of same type as CRD really... maybe can figure out how by breakpointing
		//could make that validation optional
		//if we can inspect the file then can check for whether resources of that type present

		// Load Yaml into Kubernetes resources
		//TODO: try it with URL as well as local file
		List<HasMetadata> result = kubernetesClient.load(new FileInputStream(resourceLocation)).get();


		// Apply Kubernetes Resources
		//do we need namespace like in https://stackoverflow.com/questions/53501540/kubectl-apply-f-spec-yaml-equivalent-in-fabric8-java-api
		//TODO: refactor steps out into methods
		kubernetesClient.resourceList(result).createOrReplace();

		//watch and delete when found - should we create watch before the apply?
	}

	private CustomResourceDefinition getCRD() {
		//TODO: extract name to property
		CustomResourceDefinition customResourceDefinition = kubernetesClient.customResourceDefinitions().withName(crdName).get();
		if(customResourceDefinition==null){
			System.out.println("CRD "+crdName+ " not present");
			System.exit(1);
		}
		return customResourceDefinition;
	}
}

