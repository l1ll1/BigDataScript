package org.bds.lang;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.bds.compile.CompilerMessages;
import org.bds.run.BdsThread;
import org.bds.run.RunState;
import org.bds.scope.Scope;
import org.bds.util.Gpr;

/**
 * If statement
 *
 * @author pcingola
 */
public class Switch extends Statement {

	Expression switchExpr;
	Case[] caseStatements;
	Default defaultStatement;

	public Switch(BdsNode parent, ParseTree tree) {
		super(parent, tree);
	}

	public Expression getSwitchExpr() {
		return switchExpr;
	}

	@Override
	protected void parse(ParseTree tree) {
		List<Case> caseSts = new ArrayList<>();

		int idx = 0;
		if (isTerminal(tree, idx, "switch")) idx++; // 'switch'
		if (isTerminal(tree, idx, "(")) idx++; // '('
		if (!isTerminal(tree, idx, ")")) switchExpr = (Expression) factory(tree, idx++);
		if (isTerminal(tree, idx, ")")) idx++; // ')'
		if (isTerminal(tree, idx, "{")) idx++; // '{'
		int idxOri = idx;

		// Parse all 'case' statements
		while (true) {
			Case caseSt = new Case(this, tree);
			idx = caseSt.parse(tree, idx);
			if (idx < 0) break;
			caseSts.add(caseSt);
		}
		caseStatements = caseSts.toArray(new Case[0]);

		// Do we have an 'default' statement?
		Default defSt = new Default(this, tree);
		idx = defSt.parse(tree, idxOri);
		if (idx < 0) return; // Default statement not found
		defaultStatement = defSt;
	}

	void restoreStack(BdsThread bdsThread) {
		if (bdsThread.isCheckpointRecover()) return;
		bdsThread.pop(); // Remove case 'fall-through' result from stack
		bdsThread.pop(); // Remove switch expression result from stack
	}

	/**
	 * Run the program
	 */
	@Override
	public void runStep(BdsThread bdsThread) {
		// Run switch expression
		runSwitchExpression(bdsThread);

		if (!bdsThread.isCheckpointRecover()) {
			// Put the fall-through value in the stack
			bdsThread.push(false);
		}

		// Run each of the 'case' statements
		for (Case caseSt : caseStatements) {
			caseSt.runStep(bdsThread);

			switch (bdsThread.getRunState()) {
			case OK:
			case CHECKPOINT_RECOVER:
				break;

			case BREAK: // Break from 'switch'
				bdsThread.setRunState(RunState.OK);
				restoreStack(bdsThread);
				return;

			case CONTINUE: // Continue: Breaking form a 'for' loop. Propagate 'continue' state
			case RETURN: // Return
			case EXIT: // Exit program
			case FATAL_ERROR:
				restoreStack(bdsThread);
				return;

			default:
				throw new RuntimeException("Unhandled RunState: " + bdsThread.getRunState());
			}
		}

		// Run default statement
		if (defaultStatement != null) {
			defaultStatement.runStep(bdsThread);
			// When the 'default' is in the middle of a 'switch', there can be
			// a 'break' statement. In this case we must clear the 'break' state
			// so it doesn't get propagated
			if (bdsThread.getRunState() == RunState.BREAK) bdsThread.setRunState(RunState.OK);
		}

		restoreStack(bdsThread);
	}

	/**
	 * Evaluate switch expression
	 */
	void runSwitchExpression(BdsThread bdsThread) {
		if (switchExpr == null) return;
		bdsThread.run(switchExpr);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("switch( ");
		if (switchExpr != null) sb.append(switchExpr);
		sb.append(" ) {\n");

		if (caseStatements != null) {
			for (Case c : caseStatements)
				sb.append(Gpr.prependEachLine("\t", c.toString()));
		}

		if (defaultStatement != null) {
			sb.append(Gpr.prependEachLine("\t", defaultStatement.toString()));
		}

		sb.append("\n}");

		return sb.toString();
	}

	@Override
	protected void typeCheck(Scope scope, CompilerMessages compilerMessages) {
		if (switchExpr != null) switchExpr.returnType(scope);
	}

}
