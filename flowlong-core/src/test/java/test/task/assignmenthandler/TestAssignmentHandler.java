/* Copyright 2013-2015 www.snakerflow.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.task.assignmenthandler;

import com.flowlong.bpm.engine.entity.Instance;
import com.flowlong.bpm.engine.entity.Task;
import org.junit.jupiter.api.Test;
import test.mysql.MysqlTest;

import java.util.List;

public class TestAssignmentHandler extends MysqlTest {

	@Test
	public void test() {
		Long processId = this.deployByResource("test/task/assignmenthandler/assignmentthandler.long");
		Instance instance = flowLongEngine.startInstanceById(processId, "2");
		System.out.println("instance=" + instance);
		List<Task> tasks = flowLongEngine.queryService().getActiveTasksByInstanceId(instance.getId());
		for(Task task : tasks) {
			flowLongEngine.executeTask(task.getId(), "admin");
		}
	}
}