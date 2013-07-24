package ca.mcgill.mcb.pcingola.bigDataScript.lang;

import org.antlr.v4.runtime.tree.ParseTree;

import ca.mcgill.mcb.pcingola.bigDataScript.run.BigDataScriptThread;
import ca.mcgill.mcb.pcingola.bigDataScript.scope.Scope;
import ca.mcgill.mcb.pcingola.bigDataScript.scope.ScopeSymbol;
import ca.mcgill.mcb.pcingola.bigDataScript.util.CompilerMessage.MessageType;
import ca.mcgill.mcb.pcingola.bigDataScript.util.CompilerMessages;

/**
 * A variable reference
 * 
 * @author pcingola
 */
public class VarReference extends Expression {

	String name;

	public VarReference(BigDataScriptNode parent, ParseTree tree) {
		super(parent, tree);
	}

	/**
	 * Evaluate an expression
	 */
	@Override
	public Object eval(BigDataScriptThread csThread) {
		ScopeSymbol ss = csThread.getScope().getSymbol(name);
		return ss.getValue();
	}

	/**
	 * Get symbol from scope
	 * @param scope
	 * @return
	 */
	public ScopeSymbol getScopeSymbol(Scope scope) {
		return scope.getSymbol(name);
	}

	@Override
	protected boolean isReturnTypesNotNull() {
		return returnType != null;
	}

	@Override
	protected void parse(ParseTree tree) {
		name = tree.getChild(0).getText();
	}

	@Override
	public Type returnType(Scope scope) {
		if (returnType != null) return returnType;

		ScopeSymbol ss = scope.getSymbol(name, false);
		if (ss == null) return null; // Symbol not found

		returnType = ss.getType();
		return returnType;
	}

	@Override
	protected void typeCheck(Scope scope, CompilerMessages compilerMessages) {
		// Calculate return type
		returnType(scope);

		if (!scope.hasSymbol(name, false)) compilerMessages.add(this, "Symbol '" + name + "' cannot be resolved", MessageType.ERROR);
	}

	@Override
	protected void typeCheckNotNull(Scope scope, CompilerMessages compilerMessages) {
		throw new RuntimeException("This method should never be called!");
	}

}