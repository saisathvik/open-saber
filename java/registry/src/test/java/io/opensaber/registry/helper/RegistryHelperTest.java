package io.opensaber.registry.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.DecryptionHelper;
import io.opensaber.registry.service.IReadService;
import io.opensaber.registry.service.ISearchService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.util.ViewTemplateManager;
import io.opensaber.validators.IValidate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.List;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
@SpringBootTest(classes = { ObjectMapper.class})
public class RegistryHelperTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@InjectMocks
	private RegistryHelper registerHelper;

	@Autowired
	private ObjectMapper objectMapper;

	@Mock
	private ISearchService searchService;
	
	@Mock
	private ViewTemplateManager viewTemplateManager;
	
	@Mock
    private ShardManager shardManager;

	@Mock
	RegistryService registryService;

	@Mock
	IReadService readService;

	@Mock
	private DBConnectionInfoMgr dbConnectionInfoMgr;

	@Mock
	private DecryptionHelper decryptionHelper;

	@Mock
	private IValidate validationService;

	@Mock
	private EntityStateHelper entityStateHelper;

	@Captor
	private ArgumentCaptor<String> stringArgumentCaptor;

	@Before
	public void initMocks() {
		ReflectionTestUtils.setField(registerHelper, "auditSuffix", "Audit");
		ReflectionTestUtils.setField(registerHelper, "auditSuffixSeparator", "_");
		ReflectionTestUtils.setField(registerHelper, "uuidPropertyName", "osid");
		ReflectionTestUtils.setField(registerHelper, "objectMapper", objectMapper);
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void getAuditLogTest() throws Exception {

		// Data creation
		String inputJson = "{\"Teacher\":{ \"filters\":{ \"recordId\":{\"eq\":\"12c61cc3-cc6a-4a96-8409-e506fb26ddbb\"} } } }";

		String result = "{ \"Teacher_Audit\": [{ \"auditId\": \"66fecb96-b87c-44b5-a930-3de96503aa13\", \"recordId\": \"12c61cc3-cc6a-4a96-8409-e506fb26ddbb\","
				+ " \"timeStamp\": \"2019-12-23 16:56:50.905\", \"date\": 1578566074000, \"@type\": \"Teacher_Audit\", \"action\": \"ADD\", "
				+ "\"auditJson\": [ \"op\", \"path\" ], \"osid\": \"1-d28fd315-bc28-4db0-b7f8-130ff164ba01\", \"userId\": \"35448199-0a7b-4491-a796-b053b9fcfd29\","
				+ " \"transactionId\": [ 870924631 ] }] }";

		JsonNode jsonNode = null;
		JsonNode resultNode = null;
		jsonNode = objectMapper.readTree(inputJson);
		resultNode = objectMapper.readTree(result);

		Mockito.when(searchService.search(ArgumentMatchers.any())).thenReturn(resultNode);
		Mockito.when(viewTemplateManager.getViewTemplate(ArgumentMatchers.any())).thenReturn(null);

		JsonNode node = registerHelper.getAuditLog(jsonNode);
		Assert.assertEquals(jsonNode.get("Teacher").get("filters").get("recordId").get("eq"), node.get("Teacher_Audit").get(0).get("recordId"));
	}


	@Test
	public void addEntityPropertyTest() throws Exception {
		JsonNode tests = objectMapper.readTree(new File("src/test/resources/addPropTests.json"));
		JsonNode existingNode = tests.get("existingNode");

		RegistryHelper registryHelperSpy = Mockito.spy(registerHelper);
		Mockito.doReturn(existingNode).when(registryHelperSpy).readEntity(
				ArgumentMatchers.any(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any(),
				ArgumentMatchers.eq(false),
				ArgumentMatchers.isNull(),
				ArgumentMatchers.eq(false)
		);
		Mockito.when(dbConnectionInfoMgr.getUuidPropertyName()).thenReturn("osid");
		ArrayNode testScenarios = (ArrayNode) tests.get("scenarios");
		for (JsonNode tc : testScenarios) {
			registryHelperSpy.addEntityProperty(
					"Student",
					"123",
					tc.get("propertyPath").asText(),
					tc.get("newPropertyNode")
			);

		}

		Mockito.verify(registryService, Mockito.times(testScenarios.size())).updateEntity(
				ArgumentMatchers.any(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any(),
				stringArgumentCaptor.capture()
		);
		List<String> actualUpdatedNodes = stringArgumentCaptor.getAllValues();
		for (int i = 0; i < actualUpdatedNodes.size(); i++) {
			JsonNode actualNode = objectMapper.readTree(actualUpdatedNodes.get(i));
			JsonNode expected = testScenarios.get(i).get("expected");
			Assert.assertEquals(actualNode, expected);
		}
	}

	@Test
	public void updateEntityPropertyTests() throws Exception {
		JsonNode tests = objectMapper.readTree(new File("src/test/resources/updatePropTests.json"));
		JsonNode existingNode = tests.get("existingNode");

		RegistryHelper registryHelperSpy = Mockito.spy(registerHelper);
		Mockito.doReturn(existingNode).when(registryHelperSpy).readEntity(
				ArgumentMatchers.any(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any(),
				ArgumentMatchers.eq(false),
				ArgumentMatchers.isNull(),
				ArgumentMatchers.eq(false)
		);
		Mockito.when(dbConnectionInfoMgr.getUuidPropertyName()).thenReturn("osid");
		ArrayNode testScenarios = (ArrayNode) tests.get("scenarios");
		for (JsonNode tc : testScenarios) {
			registryHelperSpy.updateEntityProperty(
					"Student",
					"123",
					tc.get("propertyPath").asText(),
					tc.get("updatedPropNode")
			);

		}

		Mockito.verify(registryService, Mockito.times(testScenarios.size())).updateEntity(
				ArgumentMatchers.any(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any(),
				stringArgumentCaptor.capture()
		);
		List<String> actualUpdatedNodes = stringArgumentCaptor.getAllValues();
		for (int i = 0; i < actualUpdatedNodes.size(); i++) {
			JsonNode actualNode = objectMapper.readTree(actualUpdatedNodes.get(i));
			JsonNode expected = testScenarios.get(i).get("expected");
			Assert.assertEquals(actualNode, expected);
		}
	}

}