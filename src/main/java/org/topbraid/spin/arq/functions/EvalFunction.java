/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package org.topbraid.spin.arq.functions;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;

/**
 * The SPARQL function spin:eval.
 * 
 * The first argument is a SPIN expression, e.g. a function call or variable.
 * All other arguments must come in pairs, alternating between an argument property
 * and its value, e.g.
 * 
 *  	spin:eval(ex:myInstance, sp:arg3, "value")
 *  
 * The expression will be evaluated with all bindings from the property-value pairs above.
 * 
 * @author Holger Knublauch
 */
public class EvalFunction extends AbstractEvalFunction {
	
	
	public EvalFunction() {
		super(1);
	}

	
	@Override
	public NodeValue exec(Node[] nodes, FunctionEnv env) {

		Model baseModel = ModelFactory.createModelForGraph(env.getActiveGraph());
		return exec(baseModel, nodes, env);
	}
}
