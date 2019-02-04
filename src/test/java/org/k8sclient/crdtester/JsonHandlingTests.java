package org.k8sclient.crdtester;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.k8sclient.crdtester.model.CustomResourceImpl;
import org.k8sclient.crdtester.services.WatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static net.javacrumbs.jsonunit.core.Option.TREATING_NULL_AS_ABSENT;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CrdtesterApplication.class,
		initializers = ConfigFileApplicationContextInitializer.class)
public class JsonHandlingTests {

	private ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${deploy.resource.location}")
	private String resourceLocation;

	@Autowired
	private WatcherService watcherService;

	@Value("${predelete-condition}")
	private String predeleteCondition;

	@Test
	public void genericCustomResourceModelCapturesJson() throws Exception {
		// given
		File file = resourceLoader.getResource(resourceLocation).getFile();
		final Reader reader = new InputStreamReader(new FileInputStream(file.getAbsoluteFile()));

		final String originalCustomResourceJson = FileCopyUtils.copyToString(reader);

		// when
		final CustomResourceImpl pod = objectMapper.readValue(originalCustomResourceJson, CustomResourceImpl.class);
		final String serializeCustomResourceAsJson = objectMapper.writeValueAsString(pod);

		// then
		assertThatJson(serializeCustomResourceAsJson).when(IGNORING_ARRAY_ORDER, TREATING_NULL_AS_ABSENT, IGNORING_EXTRA_FIELDS)
				.isEqualTo(originalCustomResourceJson);
	}

	@Test
	public void testSpringExpressionForPrecondition() throws IOException {
		File file = resourceLoader.getResource("classpath:testmodel.json").getFile();
		final Reader reader = new InputStreamReader(new FileInputStream(file.getAbsoluteFile()));

		final String testJson = FileCopyUtils.copyToString(reader);


		Assert.assertTrue(watcherService.checkExpression(objectMapper.readValue(testJson,CustomResourceImpl.class),predeleteCondition));

		Assert.assertFalse(watcherService.checkExpression(objectMapper.readValue(testJson,CustomResourceImpl.class),"unknownFields['bob'] eq 'Fred'"));
	}

}

