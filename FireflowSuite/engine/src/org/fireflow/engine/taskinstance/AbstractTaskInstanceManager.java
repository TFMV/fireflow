/**
 * Copyright 2007-2008 陈乜云（非也,Chen Nieyun）
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses. *
 */
package org.fireflow.engine.taskinstance;


import org.fireflow.engine.EngineException;
import org.fireflow.engine.ITaskInstance;
import org.fireflow.engine.impl.TaskInstance;
import org.fireflow.engine.ou.IAssignmentHandler;
import org.fireflow.engine.persistence.IPersistenceService;
import org.fireflow.kenel.IActivityInstance;
import org.fireflow.kenel.IToken;
import org.fireflow.kenel.KenelException;
import org.fireflow.model.net.Activity;
import org.fireflow.model.reference.Participant;
import org.fireflow.model.Task;

import org.fireflow.engine.RuntimeContext;

/**
 * @author chennieyun
 * 
 */
public abstract class AbstractTaskInstanceManager implements
        ITaskInstanceManager {

    /*
     * (non-Javadoc)
     * 
     * @see org.fireflow.engine.taskinstance.ITaskInstanceManager#archiveTaskInstances(org.fireflow.kenel.IActivityInstance)
     */
    public void archiveTaskInstances(IActivityInstance activityInstance) {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fireflow.engine.taskinstance.ITaskInstanceManager#createTaskInstances(org.fireflow.kenel.IActivityInstance)
     */
    public void createTaskInstances(IToken token,
            IActivityInstance activityInstance) throws EngineException ,KenelException{

        // TODO Auto-generated method stub
        Activity activity = activityInstance.getActivity();
        RuntimeContext ctx = RuntimeContext.getInstance();
        IPersistenceService persistenceService = ctx.getPersistenceService();

//		Package pkg = activity.getWorkflowProcess().getPackage();
        for (int i = 0; i < activity.getTasks().size(); i++) {
            Task task = activity.getTasks().get(i);
            // 1、创建Task实例，并设置工作流系统定义的属性
            ITaskInstance taskInstance = this.createTaskInstance(token, task,
                    activity);
            ((TaskInstance) taskInstance).setProcessInstance(token.getProcessInstance());
            ((TaskInstance) taskInstance).setActivityId(activity.getId());
            ((TaskInstance) taskInstance).setCompletionStrategy(task.getAssignmentStrategy());
            ((TaskInstance) taskInstance).setCreatedTime(ctx.getSysDate());
            ((TaskInstance) taskInstance).setDisplayName(task.getDisplayName());
            ((TaskInstance) taskInstance).setName(task.getName());
            if (task.getStartMode().equals(Task.AUTOMATIC)) {
                ((TaskInstance) taskInstance).setState(TaskInstance.STARTED);
            } else {
                ((TaskInstance) taskInstance).setState(TaskInstance.INITIALIZED);
            }
            ((TaskInstance) taskInstance).setTaskId(task.getId());
            ((TaskInstance) taskInstance).setTaskType(task.getType());

            // 2、保存实例
            persistenceService.saveTaskInstance(taskInstance);
//			token.getProcessInstance().getTaskInstances().add(taskInstance);

            // 3、分配任务

            if (task.getType().equals(Task.FORM)) {
                // performer(id,name,type,handler)
                Participant performer = task.getPerformer();
                if (performer == null || performer.getAssignmentHandler().trim().equals("")) {
                    throw new EngineException(
                            "流程定义错误，Form类型的 task必须指定performer及其AssignmentHandler");
                }
                assign(taskInstance, performer);
            }

            // 4、如果startmode=auto，则启动任务,仅对subflow和Tool类型的task有效
            // 对于 manual类型的task ，只能够自动签收
            // startMode是否可以去掉？没有什么意义。mual类型的task的自动起动可以通过workitem的eventhandler实现

            if (Task.SUBFLOW.equals(task.getType()) || Task.TOOL.equals(task.getType())) {
                taskInstance.start();
//                task.get
            }

        }
    }

    public abstract ITaskInstance createTaskInstance(IToken token, Task task,
            Activity activity);

    // TODO asignmentHandler是否可以缓存？
    protected void assign(ITaskInstance taskInstance, Participant part) {
        String asignmentHandlerClassName = part.getAssignmentHandler();
        try {
            Class clz = Class.forName(asignmentHandlerClassName);
            Object assignmentHandler = clz.newInstance();
            ((IAssignmentHandler) assignmentHandler).assign(taskInstance, part.getName());
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (EngineException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
