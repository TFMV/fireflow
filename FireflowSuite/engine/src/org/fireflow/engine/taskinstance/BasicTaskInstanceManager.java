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
import org.fireflow.engine.RuntimeContext;
import org.fireflow.engine.impl.TaskInstance;
import org.fireflow.kenel.IToken;
import org.fireflow.model.Task;
import org.fireflow.model.net.Activity;

/**
 * @author chennieyun
 * 
 */
public class BasicTaskInstanceManager extends AbstractTaskInstanceManager {

    /*
     * (non-Javadoc)
     * 
     * @see org.fireflow.engine.taskinstance.AbstractTaskInstanceManager#createTaskInstance(org.fireflow.kenel.IToken,
     *      org.fireflow.model.task.Task)
     */
    @Override
    public ITaskInstance createTaskInstance(IToken token, Task task, Activity activity)throws EngineException {

        TaskInstance taskInstance = new TaskInstance();

        return taskInstance;

    }
}
