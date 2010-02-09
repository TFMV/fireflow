﻿using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using ISM.FireWorkflow.Engine;
using ISM.FireWorkflow.Model;
using ISM.FireWorkflow.Model.Io;



namespace ISM.FireWorkflow.Engine.Definition
{
    /// <summary>
    /// 从文件系统读取流程定义文件，该类忽略流程定义文件的版本，主要用于开发阶段
    /// </summary>
    public class DefinitionService4FileSystem : IDefinitionService
    {
        protected RuntimeContext rtCtx = null;

        protected Dictionary<String, WorkflowDefinition> workflowDefinitionMap = new Dictionary<String, WorkflowDefinition>();// 流程名到流程定义的id
        protected Dictionary<String, String> latestVersionKeyMap = new Dictionary<String, String>();




        /*
         * (non-Javadoc)
         * 
         * @see org.fireflow.engine.definition.IDefinitionService#getWorkflowProcess(java.lang.String)
         */

        //    public WorkflowProcess getWorkflowProcessByName(String name) {
        //        // TODO Auto-generated method stub
        //        return workflowProcessMap.get(name);
        //    }


        public void setDefinitionFiles(List<String> workflowProcessFileNames)// throws IOException, FPDLParserException,EngineException 
        {
            if (workflowProcessFileNames != null)
            {
                Dom4JFPDLParser parser = new Dom4JFPDLParser();
                //            JAXP_FPDL_Parser parser = new JAXP_FPDL_Parser();
                for (int i = 0; i < workflowProcessFileNames.Count; i++)
                {
                    Stream inStream = new FileStream(
                            workflowProcessFileNames[i].Trim(),FileMode.Open);
                    if (inStream == null)
                    {
                        throw new IOException("没有找到名称为" + workflowProcessFileNames[i] + "的流程定义文件");
                    }


                    WorkflowProcess workflowProcess = parser.parse(inStream);

                    WorkflowDefinition workflowDef = new WorkflowDefinition();
                    workflowDef.setVersion(1);

                    workflowDef.setWorkflowProcess(workflowProcess);

                    String latestVersionKey = workflowProcess.getId() + "_V_" + workflowDef.getVersion();
                    workflowDefinitionMap.Add(latestVersionKey, workflowDef);
                    latestVersionKeyMap.Add(workflowProcess.getId(), latestVersionKey);
                    //                workflowProcessMap.put(workflowProcess.getName(), workflowProcess);
                    //
                    //                List<Activity> activities = workflowProcess.getActivities();
                    //                for (int k = 0; activities != null && k < activities.Count; k++) {
                    //                    Activity activity = activities.get(k);
                    //                    activityMap.put(activity.getId(), activity);
                    //
                    //                    List<Task> tasks = activity.getTasks();
                    //                    for (int j = 0; tasks != null && j < tasks.Count; j++) {
                    //                        Task task = tasks.get(j);
                    //                        taskMap.put(task.getId(), task);
                    //                    }
                    //                }
                    //
                    //                List<Transition> transitions = workflowProcess.getTransitions();
                    //                for (int k = 0; transitions != null && k < transitions.Count; k++) {
                    //                    Transition trans = transitions.get(k);
                    //                    transitionMap.put(trans.getId(), trans);
                    //                }
                    //
                    //                List<DataField> datafields = workflowProcess.getDataFields();
                    //                for (int k = 0; datafields != null && k < datafields.Count; k++) {
                    //                    DataField df = datafields.get(k);
                    //                    dataFieldMap.put(df.getId(), df);
                    //                }

                }
            }

        }

        public List<WorkflowDefinition> getAllLatestVersionsOfWorkflowDefinition()
        {
            return new List<WorkflowDefinition>(workflowDefinitionMap.Values);
        }

        public WorkflowDefinition getWorkflowDefinitionByProcessIdAndVersionNumber(String processId, Int32 version)
        {
            return this.workflowDefinitionMap[processId + "_V_" + version];
        }

        public WorkflowDefinition getTheLatestVersionOfWorkflowDefinition(String processId)
        {
            return this.workflowDefinitionMap[this.latestVersionKeyMap[processId]];
        }

        public void setRuntimeContext(RuntimeContext ctx)
        {
            this.rtCtx = ctx;
        }
        public RuntimeContext getRuntimeContext()
        {
            return this.rtCtx;
        }
        //    public List<WorkflowProcess> getAllWorkflowProcesses() {
        //        return new ArrayList(workflowProcessMap.values());
        //    }
        //
        //    public WorkflowProcess getWorkflowProcessById(String id) {
        //        return workflowProcessMap.get(id);
        //    }

        //    public Activity getActivityById(String id) {
        //        return activityMap.get(id);
        //    }
        //
        //    public Task getTaskById(String id) {
        //        return taskMap.get(id);
        //    }
        //
        //    public Transition getTransitionById(String id) {
        //        return transitionMap.get(id);
        //    }
        //
        //    public DataField getDataFieldById(String id) {
        //        return dataFieldMap.get(id);
        //    }

        #region IDefinitionService 成员


        public WorkflowDefinition getWorkflowDefinitionByProcessIdAndVersionNumber(string processId, int? version)
        {
            throw new NotImplementedException();
        }

        #endregion
    }
}
