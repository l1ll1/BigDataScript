package ca.mcgill.mcb.pcingola.bigDataScript.lang;

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;

import ca.mcgill.mcb.pcingola.bigDataScript.run.BigDataScriptThread;
import ca.mcgill.mcb.pcingola.bigDataScript.run.RunState;

/**
 * A "wait" statement
 * 
 * @author pcingola
 */
public class Wait extends Statement {

	Expression taskId;

	public Wait(BigDataScriptNode parent, ParseTree tree) {
		super(parent, tree);
	}

	@Override
	protected void parse(ParseTree tree) {
		// child[0] = 'wait'
		if (tree.getChildCount() > 1) taskId = (Expression) factory(tree, 1);
	}

	/**
	 * Run the program
	 */
	@Override
	protected RunState runStep(BigDataScriptThread csThread) {
		if (csThread.getRunState() == RunState.WAIT_RECOVER) return runStepWaitRecover(csThread);
		return runStepOk(csThread);
	}

	/**
	 * Run in 'OK' state
	 * @param csThread
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected RunState runStepOk(BigDataScriptThread csThread) {
		boolean ok = false;

		// No arguments? Wait for all tasks
		if (taskId == null) {
			ok = csThread.waitTasksAll();
		} else {
			// Wait for a specific task or list of tasks
			Object val = taskId.eval(csThread);

			// Are we waiting for one task or a list of tasks?
			if (val instanceof List) ok = csThread.waitTasks((List) val);
			else ok = csThread.waitTask(val.toString());
		}

		// Any task failed?
		if (!ok) {
			// Create a checkpoint 
			csThread.fatalError(this, "Task/s failed.");
			return RunState.FATAL_ERROR;
		}

		return RunState.OK;
	}

	/**
	 * Run in 'WAIT_RECOVER' state.
	 * This happens when recovering from a checkpoint.
	 * 
	 * @param csThread
	 * @return
	 */
	protected RunState runStepWaitRecover(BigDataScriptThread csThread) {

		// We moved this funtionality to BigDataScriptThread

		//		//---
		//		// Find all tasks that need to be restarted
		//		//---
		//		ArrayList<Task> failedTasks = new ArrayList<Task>();
		//		for (Task t : csThread.getTasks()) {
		//			// Add all tasks that failed and are not supposed to fail
		//			if (t.isFailed() && !t.isCanFail()) failedTasks.add(t);
		//		}
		//
		//		//---
		//		// Restarts each task  
		//		//---
		//		if (!failedTasks.isEmpty()) {
		//			System.err.println("Re-executing " + failedTasks.size() + " failed tasks:");
		//			for (Task t : failedTasks) {
		//				System.err.println(t);
		//				ExpressionTask.execute(csThread, t);
		//			}
		//		}

		return runStepOk(csThread);
	}
}
