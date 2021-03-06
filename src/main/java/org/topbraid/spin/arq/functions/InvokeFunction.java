/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package org.topbraid.spin.arq.functions;

import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.arq.AbstractFunction;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionFactory;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.util.FmtUtils;

/**
 * The function spif:invoke.
 * 
 * @author Holger Knublauch
 */
public class InvokeFunction	extends AbstractFunction {

	@Override
	protected NodeValue exec(Node[] nodes, FunctionEnv env) {
		if(nodes.length == 0) {
			throw new ExprEvalException("Missing function URI argument");
		}
		Node commandNode = nodes[0];
		if(!commandNode.isURI()) {
			throw new ExprEvalException("First argument must be the URI of a function");
		}
		
		String uri = commandNode.getURI();
		
		// Special handling of SPARQL system functions such as sp:gt
		Resource functionResource = SPL.getModel().getResource(uri);
		if(SP.NS.equals(functionResource.getNameSpace())) {
			Statement symbolS = functionResource.getProperty(SPIN.symbol);
			if(symbolS != null) {
				PrefixMapping pm = env.getActiveGraph() != null ? env.getActiveGraph().getPrefixMapping() : new PrefixMappingImpl();
				final String varName = "result";
				StringBuffer sb = new StringBuffer();
				sb.append("SELECT ?" + varName + " \n");
				sb.append("WHERE {\n");
				sb.append("    BIND (");
				sb.append(FmtUtils.stringForNode(nodes[1], pm));
				sb.append(" ");
				sb.append(symbolS.getString());
				sb.append(" ");
				sb.append(FmtUtils.stringForNode(nodes[2], pm));
				sb.append(" AS ?" + varName + ") . \n");
				sb.append("}");
				
				Model model = ModelFactory.createModelForGraph(env.getActiveGraph());
				Query arq = ARQFactory.get().createQuery(model, sb.toString());
				try(QueryExecution qexec = ARQFactory.get().createQueryExecution(arq, model)) {
				    ResultSet rs = qexec.execSelect();
				    if(rs.hasNext()) {
				        RDFNode result = rs.next().get(varName);
				        if(result != null) {
				            return NodeValue.makeNode(result.asNode());
				        }
				    }
				    throw new ExprEvalException("Failed to evaluate function - empty result set");
				}
			}
		}
		
		FunctionFactory ff = FunctionRegistry.get().get(uri);
		if(ff == null) {
			throw new ExprEvalException("Unknown function " + uri);
		}
		
		Function function = ff.create(uri);
		ExprList exprList = new ExprList();
		
		for(int i = 1; i < nodes.length; i++) {
			Node node = nodes[i];
			if(node != null) {
				exprList.add(NodeValue.makeNode(node));
			}
			else {
				exprList.add(null);
			}
		}
		
		NodeValue result = function.exec(new BindingHashMap(), exprList, uri, env);
		return result;
	}
}
