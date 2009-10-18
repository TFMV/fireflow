/*
 * Copyright 2007-2009 非也
 * All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation。
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses.
 */
package org.fireflow.engine;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.fireflow.engine.persistence.IFireWorkflowHelperDao;
import org.fireflow.engine.persistence.IPersistenceService;
import org.fireflow.engine.persistence.hibernate.FireWorkflowHelperDao;
import org.fireflow.engine.taskinstance.CurrentUserAssignmentHandlerMock;
import org.fireflow.kernel.IToken;
import org.fireflow.kernel.KernelException;
import org.junit.BeforeClass;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.Assert.*;

/**
 *
 * @author 非也
 * @version 1.0
 * Created on Apr 16, 2009
 */
public class SuspendProcessInstanceTest {

    private final static String springConfigFile = "config/TestContext.xml";
    private static ClassPathResource resource = null;
    private static XmlBeanFactory beanFactory = null;
    private static TransactionTemplate transactionTemplate = null;
    private static RuntimeContext runtimeContext = null;

    //--------constant----------------------
    //客户电话，用于控制是否执行“发送手机短信通知客户收货”。通过设置mobile等于null和非null值分别进行测试。
    private final static String mobile = null;//"123123123123";

    //-----variables-----------------
    static IProcessInstance currentProcessInstance = null;
    static String paymentWorkItemId = null;
    static String prepareGoodsWorkItemId = null;
    static String deliverGoodsWorkItemId = null;

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
            	IFireWorkflowHelperDao helperDao = (IFireWorkflowHelperDao) beanFactory.getBean("FireWorkflowHelperDao");
                helperDao.clearAllTables();
            }
        });
    }

    /**
     * 创建流程实例，并执行实例的run方法。
     */
    @Test
    public void testSuspendProcessInstance() {
        System.out.println("--------------Start a new process ----------------");
        currentProcessInstance = (IProcessInstance) transactionTemplate.execute(new TransactionCallback() {

            public Object doInTransaction(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    //启动"/workflowdefinition/example_workflow.xml"中的“送货流程”
                    IProcessInstance processInstance = workflowSession.createProcessInstance("Goods_Deliver_Process","fireflowTester");
                    processInstance.setProcessInstanceVariable("mobile", mobile);
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
        List taskInstanceList = persistenceService.findTaskInstancesForProcessInstance(currentProcessInstance.getId(), "Goods_Deliver_Process.PaymentActivity");
        assertNotNull(taskInstanceList);
        assertEquals(1, taskInstanceList.size());
        assertEquals(new Integer(ITaskInstance.RUNNING), ((ITaskInstance) taskInstanceList.get(0)).getState());
        assertEquals(1, ((ITaskInstance) taskInstanceList.get(0)).getStepNumber().intValue());

        List tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(1, tokenList.size());
        IToken token = (IToken) tokenList.get(0);
        assertEquals(1, token.getStepNumber().intValue());

        List workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "Goods_Deliver_Process", "Goods_Deliver_Process.PaymentActivity.Payment_Task");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.RUNNING), ((IWorkItem) workItemList.get(0)).getState());

        paymentWorkItemId = ((IWorkItem) workItemList.get(0)).getId();

        //abort processInstance
        transactionTemplate.execute(new TransactionCallback() {

            public Object doInTransaction(TransactionStatus arg0) {
                try {
                    currentProcessInstance.suspend();
                } catch (EngineException ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        });

        assertEquals(IProcessInstance.RUNNING, currentProcessInstance.getState().intValue());
        assertEquals(Boolean.TRUE, currentProcessInstance.isSuspended());

        taskInstanceList = persistenceService.findTaskInstancesForProcessInstance(currentProcessInstance.getId(), "Goods_Deliver_Process.PaymentActivity");
        assertNotNull(taskInstanceList);
        assertEquals(1, taskInstanceList.size());
        assertEquals(new Integer(ITaskInstance.RUNNING), ((ITaskInstance) taskInstanceList.get(0)).getState());
        assertEquals(Boolean.TRUE, ((ITaskInstance) taskInstanceList.get(0)).isSuspended());
        assertEquals(1, ((ITaskInstance) taskInstanceList.get(0)).getStepNumber().intValue());


        workItemList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "Goods_Deliver_Process", "Goods_Deliver_Process.PaymentActivity.Payment_Task");
        assertNotNull(workItemList);
        assertEquals(1, workItemList.size());
        assertEquals(new Integer(ITaskInstance.RUNNING), ((IWorkItem) workItemList.get(0)).getState());


        tokenList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokenList);
        assertEquals(1, tokenList.size());
    }

    /**
     * 结束"收银岗"的workItem
     */
    @Test(expected = RuntimeException.class)
    public void testCompletePaymentWorkItem() {
        System.out.println("--------------Complete Payment WorkItem ----------------");
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem paymentTaskWorkItem = workflowSession.findWorkItemById(paymentWorkItemId);
                    paymentTaskWorkItem.complete();
                } catch (EngineException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException();
                } catch (KernelException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException();
                }
            }
        });

        IPersistenceService persistenceService = runtimeContext.getPersistenceService();
        IWorkItem wi = persistenceService.findWorkItemById(paymentWorkItemId);
        assertEquals(new Integer(IWorkItem.COMPLETED), wi.getState());

        List tokensList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokensList);
        assertEquals(1, tokensList.size());
        IToken token = (IToken) tokensList.get(0);
        assertEquals(2, token.getStepNumber().intValue());

        List todoWorkItemsList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "Goods_Deliver_Process", "Goods_Deliver_Process.PrepareGoodsTask");
        assertNotNull(todoWorkItemsList);
        assertEquals(1, todoWorkItemsList.size());
        prepareGoodsWorkItemId = ((IWorkItem) todoWorkItemsList.get(0)).getId();
    }

    @Test
    public void restoreProcessInstance() {
        transactionTemplate.execute(new TransactionCallback() {

            public Object doInTransaction(TransactionStatus arg0) {
                try {
                    currentProcessInstance.restore();

                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem paymentTaskWorkItem = workflowSession.findWorkItemById(paymentWorkItemId);
                    paymentTaskWorkItem.complete();
                } catch (EngineException ex) {
                    ex.printStackTrace();
                } catch (KernelException ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        });
        IPersistenceService persistenceService = runtimeContext.getPersistenceService();
        IWorkItem wi = persistenceService.findWorkItemById(paymentWorkItemId);
        assertEquals(new Integer(IWorkItem.COMPLETED), wi.getState());

        List tokensList = persistenceService.findTokensForProcessInstance(currentProcessInstance.getId(), null);
        assertNotNull(tokensList);
        assertEquals(1, tokensList.size());
        IToken token = (IToken) tokensList.get(0);
        assertEquals(2, token.getStepNumber().intValue());

        List todoWorkItemsList = persistenceService.findTodoWorkItems(CurrentUserAssignmentHandlerMock.ACTOR_ID, "Goods_Deliver_Process", "Goods_Deliver_Process.PrepareGoodsTask");
        assertNotNull(todoWorkItemsList);
        assertEquals(1, todoWorkItemsList.size());
        prepareGoodsWorkItemId = ((IWorkItem) todoWorkItemsList.get(0)).getId();

    }

    @Test
    public void testSuspendTaskInstance() {
        transactionTemplate.execute(new TransactionCallback() {

            public Object doInTransaction(TransactionStatus arg0) {
                try {
                    IWorkflowSession session = runtimeContext.getWorkflowSession();
                    ITaskInstance taskInstance = (ITaskInstance) session.execute(new IWorkflowSessionCallback() {

                        public Object doInWorkflowSession(RuntimeContext ctx) throws EngineException, KernelException {
                            IPersistenceService persistenceService = runtimeContext.getPersistenceService();
                            List taskInstanceList = persistenceService.findTaskInstancesForProcessInstance(currentProcessInstance.getId(), "Goods_Deliver_Process.PrepareGoodsActivity");
                            ITaskInstance taskInstance = (ITaskInstance) taskInstanceList.get(0);
                            return taskInstance;
                        }
                    });
                    taskInstance.suspend();

                } catch (EngineException ex) {

                    ex.printStackTrace();
                } catch (KernelException ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        });

        IPersistenceService persistenceService = runtimeContext.getPersistenceService();
        List taskInstanceList = persistenceService.findTaskInstancesForProcessInstance(currentProcessInstance.getId(), "Goods_Deliver_Process.PrepareGoodsActivity");
        assertNotNull(taskInstanceList);
        assertEquals(1, taskInstanceList.size());
        ITaskInstance taskInstance = (ITaskInstance) taskInstanceList.get(0);
        assertEquals(Boolean.TRUE, taskInstance.isSuspended());


    }

    @Test(expected = RuntimeException.class)
    public void testClaimPrepareGoodsWorkItem() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                try {
                    IWorkflowSession workflowSession = runtimeContext.getWorkflowSession();
                    IWorkItem prepareGoodsWorkItem = workflowSession.findWorkItemById(prepareGoodsWorkItemId);
                    prepareGoodsWorkItem.claim();
                } catch (EngineException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException();
                } catch (KernelException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException();
                }
            }
        });
    }
}
