﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using FireWorkflow.Net.Model;
using FireWorkflow.Net.Model.Net;
using FireWorkflow.Net.Engine.Taskinstance;

namespace FireWorkflow.Net.Engine
{
    public enum TaskInstanceStateEnum
    {
        /// <summary>初始化状态</summary>
        INITIALIZED = 0,
        /// <summary>运行状态</summary>
        RUNNING = 1,
        /// <summary>已经结束</summary>
        COMPLETED = 7,
        /// <summary>被撤销</summary>
        CANCELED = 9
    }

    /// <summary>
    /// 任务实例
    /// 对任务实例的状态字段作如下规定：小于5的状态为“活动”状态，大于等于5的状态为“非活动”状态。
    /// 活动状态包括：INITIALIZED,RUNNING,SUSPENDED
    /// 非活动状态包括：COMPLETED,CANCELED
    /// </summary>
    public interface ITaskInstance
    {
        /// <summary>返回任务实例的Id</summary>
        String Id { get; }

        /// <summary>返回对应的任务Id</summary>
        String TaskId { get; }

        /// <summary>返回任务Name</summary>
        String Name { get; }

        /// <summary>返回任务显示名</summary>
        String DisplayName { get; }


        /// <summary>返回对应的流程实例Id</summary>
        String ProcessInstanceId { get; }

        /// <summary>返回对应的流程的Id</summary>
        String ProcessId { get; }

        /// <summary>返回流程的版本</summary>
        Int32 Version { get; }

        /// <summary>返回任务实例创建的时间</summary>
        DateTime? CreatedTime { get; }

        /// <summary>返回任务实例启动的时间</summary>
        DateTime? StartedTime { get; }

        /// <summary>返回任务实例结束的时间</summary>
        DateTime? EndTime { get; }

        /// <summary>返回任务实例到期日期</summary>
        DateTime? ExpiredTime { get; }// 过期时间

        /// <summary>返回任务实例的状态，取值为：INITIALIZED(已初始化），STARTED(已启动),COMPLETED(已结束),CANCELD(被取消)</summary>
        TaskInstanceStateEnum State { get; }

        /// <summary>返回任务实例的分配策略，取值为 org.fireflow.model.Task.ALL或者org.fireflow.model.Task.ANY</summary>
        FormTaskEnum AssignmentStrategy { get; }

        /// <summary>返回任务实例所属的环节的Id</summary>
        String ActivityId { get; }

        /// <summary>
        /// 返回任务类型，取值为TaskTypeEnum:FORM,TOOL,SUBFLOW,DUMMY
        /// </summary>
        TaskTypeEnum TaskType { get; }

        /// <summary>当执行JumpTo和LoopTo操作时，返回目标Activity 的Id</summary>
        String TargetActivityId { get; }

        /// <summary>返回TaskInstance的"步数"。</summary>
        Int32 StepNumber { get; }
        

        /// <summary>
        /// 返回任务实例对应的流程
        /// fireflow.engine.EngineException
        /// </summary>
        WorkflowProcess getWorkflowProcess();// throws EngineException;

        /// <summary>
        /// 返回任务实例对应的Task对象
        /// fireflow.engine.EngineException
        /// </summary>
        Task getTask();// throws EngineException;

        IProcessInstance getAliveProcessInstance();

        /// <summary>
        /// 返回任务是里对应的环节
        /// fireflow.engine.EngineException
        /// </summary>
        Activity getActivity();// throws EngineException;

        /// <summary>挂起 </summary>
        void suspend();// throws EngineException;

        Boolean IsSuspended();

        /// <summary>
        /// 从挂起状态恢复到挂起前的状态
        /// fireflow.engine.EngineException
        /// </summary>
        void restore();// throws EngineException;

        /// <summary>
        /// <para>将该TaskInstance中止，并且使得当前流程实例按照流程定义往下流转。</para>
        /// <para>与该TaskInstance相关的WorkItem都被置为Canceled状态。</para>
        /// 如果该TaskInstance的状态已经是Completed或者Canceled，则抛出异常。
        /// </summary>
        void abort();

        /// <summary>
        /// 将该TaskInstance中止，并且使得当前流程实例跳转到targetActivityId指定的环节。
        /// 与该TaskInstance相关的WorkItem都被置为Canceled状态。
        /// 如果该TaskInstance的状态已经是Completed或者Canceled，则抛出异常。
        /// </summary>
        /// <param name="targetActivityId"></param>
        void abort(String targetActivityId);

        /// <summary>
        /// <para>将该TaskInstance中止，并且使得当前流程实例跳转到targetActivityId指定的环节。该环节任务的操作人从dynamicAssignmentHandler获取。</para>
        /// <para>当前环节和目标环节必须在同一条“执行线”上</para>
        /// <para>与该TaskInstance相关的WorkItem都被置为Canceled状态。</para> 
        /// 如果该TaskInstance的状态已经是Completed或者Canceled，则抛出异常。
        /// </summary>
        /// <param name="targetActivityId"></param>
        /// <param name="dynamicAssignmentHandler"></param>
        void abort(String targetActivityId, DynamicAssignmentHandler dynamicAssignmentHandler);

        /// <summary>
        /// <para>将该TaskInstance中止，并且使得当前流程实例跳转到targetActivityId指定的环节。该环节任务的操作人从dynamicAssignmentHandler获取。</para> 
        /// <para>当前环节和目标环节 可以不在 同一条“执行线”上</para> 
        /// <para>与该TaskInstance相关的WorkItem都被置为Canceled状态。</para> 
        /// 如果该TaskInstance的状态已经是Completed或者Canceled，则抛出异常。
        /// </summary>
        /// <param name="targetActivityId"></param>
        /// <param name="dynamicAssignmentHandler"></param>
        void abortEx(String targetActivityId, DynamicAssignmentHandler dynamicAssignmentHandler);

    }
}
