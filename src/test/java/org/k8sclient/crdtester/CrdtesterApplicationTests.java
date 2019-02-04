package org.k8sclient.crdtester;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CrdtesterApplication.class,
		initializers = ConfigFileApplicationContextInitializer.class)
public class CrdtesterApplicationTests {


	@Test
	public void contextLoads() {
	}

}

