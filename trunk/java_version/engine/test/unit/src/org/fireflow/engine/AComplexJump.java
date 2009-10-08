package org.fireflow.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fireflow.engine.persistence.IPersistenceService;
import org.fireflow.engine.persistence.hibernate.FireWorkflowHelperDao;
import org.fireflow.engine.taskinstance.AssignmentHandlerMock;
import org.fireflow.engine.taskinstance.CurrentUserAssignmentHandlerMock;
import org.fireflow.kernel.IToken;
import org.fireflow.kernel.KernelException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public class AComplexJump {

    private final static String springConfigFile = "config/TestContext.xml";
    private static ClassPathResource resource = null;
    private static XmlBeanFactory beanFactory = null;
    private static TransactionTemplate transactionTemplate = null;
    private static RuntimeContext runtimeContext = null;

    //--------constant----------------------
    //客户电话，用于控制是否执行“发送手机短信通知客户收货”。通过设置mobile等于null和非null值分别进行测试。
    private final static String mobile = "";//null;//"123123123123";

    //-----variables-----------------

    static IProcessInstance currentProcessInstance = null;
    static String workItemId_1 = null;
    static String workItemId_2 = null;
    static String workItemId_8 = null;    
    static String workItemId_12 = null;
    static String workItemId_13 = null;
    static String workItemId_14 = null;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        resource = new ClassPathResource(springConfigFile);
        beanFactory = new XmlBeanFactory(resource);
        transactionTemplate = (TransactionTemplate) beanFactory.getBean("transactionTemplate");
        runtimeContext = (RuntimeContext) beanFactory.getBean("runtimeContext");

        //首先将表中的数据清除
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                FireWorkflowHelperDao helperDao = (FireWorkflowHelperDao) beanFactory.getBean("FireWorkflowHelperDao");
                helperDao.clearAllTables();
            }
        });
    }
    
    /**
     * 创建流程实例，并执行实例的run方法。
     */
    @Test
    public void testStartNewProcess() {
        System.out.println("--------------Start a new process ----------------");
        currentProcessInstance = (IProcessInstance) transactionTemplate.execute(new TransactionCallback() {

            public Object doInTransaction(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    System.out.println("====================workflowSession in start process is "+workflowSession.hashCode());

                    IProcessInstance processInstance = workflowSession.createProcessInstance("AComplexJump",AssignmentHandlerMock.ACTOR_ID);

                    processInstance.run();
                    return processInstance;
                } catch (EngineException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KernelException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                }

                return null;
            }
        });
        assertNotNull(currentProcessInstance);

        IPersistenceService persistenceService = runtimeContext.getPersistenceService();

        List tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        IToken token = (IToken)tokenList.get(0);
        assertEquals(1,token.getStepNumber().intValue());

        List workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "AComplexJump", "AComplexJump.Activity1.Task1");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.RUNNING), ((IWorkItem) workItemList.get(0)).getState());

        workItemId_1 = ((IWorkItem) workItemList.get(0)).getId();

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem paymentTaskWorkItem = workflowSession.findWorkItemById(workItemId_1);
                    paymentTaskWorkItem.complete("this is a comments");
                } catch (EngineException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KernelException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(3,tokenList.size());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer4");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        
        workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "AComplexJump", "AComplexJump.Activity2.Task2");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.INITIALIZED), ((IWorkItem) workItemList.get(0)).getState());
        
        workItemId_2 = ((IWorkItem) workItemList.get(0)).getId();
    }    
    
    @Test
    public void testJumpFromAct2ToAct12() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem workItem2 = workflowSession.findWorkItemById(workItemId_2);
                    workItem2.claim();
                    workflowSession.completeWorkItemAndJumpToEx(workItemId_2, "AComplexJump.Activity12", null, "testJumpFromAct2ToAct12");
                } catch (EngineException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KernelException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        IPersistenceService persistenceService = runtimeContext.getPersistenceService();
        
        List tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(4,tokenList.size());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer4");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer9");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        List workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "AComplexJump", "AComplexJump.Activity12.Task12");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.INITIALIZED), ((IWorkItem) workItemList.get(0)).getState());
        
        workItemId_12 = ((IWorkItem) workItemList.get(0)).getId();        
    }
    
    @Test
    public void testJumpFromAct12ToAct13() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem workItem2 = workflowSession.findWorkItemById(workItemId_12);
                    workItem2.claim();
                    workflowSession.completeWorkItemAndJumpToEx(workItemId_12, "AComplexJump.Activity13", null, "testJumpFromAct12ToAct13");
                } catch (EngineException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KernelException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        IPersistenceService persistenceService = runtimeContext.getPersistenceService();
        
        List tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(4,tokenList.size());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer4");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer9");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        List workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "AComplexJump", "AComplexJump.Activity13.Task13");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.INITIALIZED), ((IWorkItem) workItemList.get(0)).getState());
        
        workItemId_13 = ((IWorkItem) workItemList.get(0)).getId();         	
    }    
    @Test
    public void testJumpFromAct13ToAct14() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem workItem2 = workflowSession.findWorkItemById(workItemId_13);
                    workItem2.claim();
                    workflowSession.completeWorkItemAndJumpToEx(workItemId_13, "AComplexJump.Activity14", null, "testJumpFromAct13ToAct14");
                } catch (EngineException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KernelException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        IPersistenceService persistenceService = runtimeContext.getPersistenceService();
        
        List tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(4,tokenList.size());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer4");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer9");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        List workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "AComplexJump", "AComplexJump.Activity14.Task14");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.INITIALIZED), ((IWorkItem) workItemList.get(0)).getState());
        
        workItemId_14 = ((IWorkItem) workItemList.get(0)).getId();       	
    }    
    
    @Test
    public void testJumpFromAct14ToAct12() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem workItem2 = workflowSession.findWorkItemById(workItemId_14);
                    workItem2.claim();
                    workflowSession.completeWorkItemAndJumpToEx(workItemId_14, "AComplexJump.Activity12", null, "testJumpFromAct14ToAct12");
                } catch (EngineException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KernelException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        IPersistenceService persistenceService = runtimeContext.getPersistenceService();
        
        List tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(4,tokenList.size());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer4");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer9");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        List workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "AComplexJump", "AComplexJump.Activity12.Task12");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.RUNNING), ((IWorkItem) workItemList.get(0)).getState());
        
        workItemId_12 = ((IWorkItem) workItemList.get(0)).getId();        	
    }    
    
    @Test
    public void testJumpFromAct12ToAct8() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem workItem2 = workflowSession.findWorkItemById(workItemId_12);
//                    workItem2.claim();
                    workflowSession.completeWorkItemAndJumpToEx(workItemId_12, "AComplexJump.Activity8", null, "testJumpFromAct14ToAct8");
                } catch (EngineException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KernelException ex) {
                    Logger.getLogger(FireWorkflowEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        IPersistenceService persistenceService = runtimeContext.getPersistenceService();
        
        List tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(5,tokenList.size());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer4");
        assertNotNull(tokenList);
        assertEquals(0,tokenList.size());
//        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer9");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());
        
        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), "AComplexJump.Synchronizer7");
        assertNotNull(tokenList);
        assertEquals(1,tokenList.size());
        assertEquals(1,((IToken)tokenList.get(0)).getValue().intValue());        
        
        List workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "AComplexJump", "AComplexJump.Activity8.Task8");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.INITIALIZED), ((IWorkItem) workItemList.get(0)).getState());
        
        workItemId_8 = ((IWorkItem) workItemList.get(0)).getId();      	
    }       
}
