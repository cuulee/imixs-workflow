package org.imixs.workflow.jee.ejb;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.SessionContext;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

/**
 * Abstract base class for jUnit tests using the WorkflowService.
 * 
 * This test class mocks a complete workflow environment without the class
 * WorkflowService
 * 
 * The test class generates a test database with process entities and activity
 * entities which can be accessed from a plug-in or the workflowKernel.
 * 
 * A JUnit Test can save, load and process workitems.
 * 
 * JUnit tests can also manipulate the model by changing entities through
 * calling the methods:
 * 
 * getActivityEntity,setActivityEntity,getProcessEntity,setProcessEntity
 * 
 * 
 * @version 2.0
 * @see AbstractPluginTest, TestWorkflowService
 * @author rsoika
 */
public class AbstractWorkflowEnvironment {
	private final static Logger logger = Logger.getLogger(AbstractWorkflowEnvironment.class.getName());
	public static final String DEFAULT_MODEL_VERSION="1.0.0";
	
	Map<String, ItemCollection> database = null;

	protected EntityService entityService;
	
	@Spy
	protected WorkflowService workflowService;

	
//	protected WorkflowService workflowService = new WorkflowService();
	protected ModelService modelService;
	protected SessionContext ctx;
	protected WorkflowContext workflowContext;
	private BPMNModel model = null;

	private String modelPath = "/bpmn/plugin-test.bpmn";

	public String getModelPath() {
		return modelPath;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	@Before
	public void setup() throws PluginException {
		MockitoAnnotations.initMocks(this);
		// setup db
		createTestDatabase();

		// mock EJBs and inject them into the workflowService EJB
		entityService = Mockito.mock(EntityService.class);
		modelService = Mockito.mock(ModelService.class);
		ctx = Mockito.mock(SessionContext.class);
	
	
		// load default model
		loadModel();

		// mock workflowService
	//	workflowService = Mockito.mock(WorkflowService.class);
		workflowContext = Mockito.mock(WorkflowContext.class);
		workflowService.entityService = entityService;
		workflowService.ctx = ctx;

	
		// simulate SessionContext ctx.getCallerPrincipal().getName()
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);

		
		// mock workflowService
		
		ModelManager modelManager = Mockito.mock(ModelManager.class);
		try {
			when (modelManager.getModel(Mockito.anyString())).thenReturn(this.getModel());
			when (modelManager.getModelByWorkitem(Mockito.any(ItemCollection.class))).thenReturn(this.getModel());
		} catch (ModelException e) {
			e.printStackTrace();
		}
		when(workflowContext.getModelManager()).thenReturn(modelManager);


		
		
		
		
		
		//workflowService.entityService = entityService;
		//workflowService.ctx = ctx;

		workflowService.modelService = modelService;
		
		try {
			when (modelService.getModel(Mockito.anyString())).thenReturn(this.getModel());
			when (modelService.getModelByWorkitem(Mockito.any(ItemCollection.class))).thenReturn(this.getModel());
			
		} catch (ModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			
//		workflowService.entityService = entityService;
//		workflowService.ctx = ctx;
	//	workflowService.modelService = modelService;
		when(workflowService.getModelManager()).thenReturn(modelService);

		
		
		
		// Simulate fineProfile("1.0.0") -> entityService.load()...
		when(entityService.load(Mockito.anyString())).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				String id = (String) args[0];
				ItemCollection result = database.get(id);
				return result;
			}
		});

		// simulate save() method
		when(entityService.save(Mockito.any(ItemCollection.class))).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				ItemCollection entity = (ItemCollection) args[0];
				database.put(entity.getItemValueString(EntityService.UNIQUEID), entity);
				return entity;
			}
		});

	
	}

	/**
	 * Create a test database with some workItems and a simple model
	 */
	protected void createTestDatabase() {

		database = new HashMap<String, ItemCollection>();

		ItemCollection entity = null;

		logger.info("createSimpleDatabase....");

		// create workitems
		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "workitem");
			entity.replaceItemValue(EntityService.UNIQUEID, "W0000-0000" + i);
			entity.replaceItemValue("txtName", "Workitem " + i);
			entity.replaceItemValue("$ModelVersion", "1.0.0");
			entity.replaceItemValue("$ProcessID", 100);
			entity.replaceItemValue("$ActivityID", 10);
			entity.replaceItemValue(WorkflowService.ISAUTHOR, true);
			database.put(entity.getItemValueString(EntityService.UNIQUEID), entity);
		}

	}

	public Model getModel() {
		return model;
	}

	public void loadModel() {
		InputStream inputStream = getClass().getResourceAsStream(this.modelPath);

		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (ModelException | ParseException | ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}