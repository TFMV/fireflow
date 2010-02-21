﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using FireWorkflow.Net.Engine;
using FireWorkflow.Net.Engine.Beanfactory;
using FireWorkflow.Net.Engine.Impl;
using FireWorkflow.Net.Engine.Persistence;
using FireWorkflow.Net.Kernel;
using FireWorkflow.Net.Model;
using FireWorkflow.Net.Model.Resource;


namespace FireWorkflow.Net.Engine.Taskinstance
{
    public class DefaultFormTaskInstanceRunner : ITaskInstanceRunner
    {

        public void run(IWorkflowSession currentSession, RuntimeContext runtimeContext, IProcessInstance processInstance, ITaskInstance taskInstance)// throws EngineException, KernelException 
        {
            if (taskInstance.TaskType!= TaskTypeEnum.FORM)//!Task.FORM.Equals(taskInstance.TaskType))
            {
                throw new EngineException(processInstance,
                        taskInstance.Activity,
                        "DefaultFormTaskInstanceRunner：TaskInstance的任务类型错误，只能为FORM类型");
            }

            DynamicAssignmentHandler dynamicAssignmentHandler = ((WorkflowSession)currentSession).consumeCurrentDynamicAssignmentHandler();
            FormTask task = (FormTask)taskInstance.Task;
            // performer(id,name,type,handler)
            Participant performer = task.Performer;
            if (performer == null || performer.AssignmentHandler.Trim().Equals(""))
            {
                throw new EngineException(processInstance,
                        taskInstance.Activity,
                        "流程定义错误，Form类型的 task必须指定performer及其AssignmentHandler");
            }
            assign(currentSession, processInstance, runtimeContext, taskInstance, task, performer, dynamicAssignmentHandler);
        }

        protected void assign(IWorkflowSession currentSession, IProcessInstance processInstance, RuntimeContext runtimeContext, ITaskInstance taskInstance, FormTask formTask, Participant part, DynamicAssignmentHandler dynamicAssignmentHandler)// throws EngineException, KernelException 
        {
            //如果有指定的Actor，则按照指定的Actor分配任务
            if (dynamicAssignmentHandler != null)
            {

                dynamicAssignmentHandler.assign((IAssignable)taskInstance, part.Name);

            }
            else
            {

                IPersistenceService persistenceService = runtimeContext.PersistenceService;
                List<ITaskInstance> taskInstanceList = persistenceService.findTaskInstancesForProcessInstance(taskInstance.ProcessInstanceId, taskInstance.ActivityId);
                ITaskInstance theLastCompletedTaskInstance = null;

                for (int i = 0; taskInstanceList != null && i < taskInstanceList.Count; i++)
                {
                    ITaskInstance tmp = (ITaskInstance)taskInstanceList[i];
                    if (tmp.Id.Equals(taskInstance.Id)) continue;
                    if (!tmp.TaskId.Equals(taskInstance.TaskId)) continue;
                    if (tmp.State != TaskInstanceStateEnum.COMPLETED) continue;
                    if (theLastCompletedTaskInstance == null)
                    {
                        theLastCompletedTaskInstance = tmp;
                    }
                    else
                    {
                        if (theLastCompletedTaskInstance.StepNumber < tmp.StepNumber)
                        {
                            theLastCompletedTaskInstance = tmp;
                        }
                    }
                }

                //如果是循环且LoopStrategy==REDO，则分配个上次完成该工作的操作员
                if (theLastCompletedTaskInstance != null && (LoopStrategyEnum.REDO.Equals(formTask.LoopStrategy) || currentSession.isInWithdrawOrRejectOperation()))
                {
                    List<IWorkItem> workItemList = persistenceService.findCompletedWorkItemsForTaskInstance(theLastCompletedTaskInstance.Id);
                    ITaskInstanceManager taskInstanceMgr = runtimeContext.TaskInstanceManager;
                    for (int k = 0; k < workItemList.Count; k++)
                    {
                        IWorkItem completedWorkItem = (IWorkItem)workItemList[k];

                        IWorkItem newFromWorkItem = taskInstanceMgr.createWorkItem(currentSession, processInstance, taskInstance, completedWorkItem.getActorId());
                        newFromWorkItem.claim();
                    }
                }
                else
                {
                    IBeanFactory beanFactory = runtimeContext.getBeanFactory();
                    IAssignmentHandler assignmentHandler = (IAssignmentHandler)beanFactory.GetBean(part.AssignmentHandler);
                    ((IAssignmentHandler)assignmentHandler).assign((IAssignable)taskInstance, part.Name);
                }
            }
        }
    }
}
