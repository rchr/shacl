/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package org.topbraid.spin.model.impl;

import org.topbraid.spin.model.TriplePath;
import org.topbraid.spin.model.print.PrintContext;
import org.topbraid.spin.model.visitor.ElementVisitor;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SP;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Mod;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathWriter;
import org.apache.jena.vocabulary.RDF;


public class TriplePathImpl extends TupleImpl implements TriplePath {

	public TriplePathImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}

	
	@Override
    public void visit(ElementVisitor visitor) {
		visitor.visit(this);
	}

	
	@Override
    public void print(PrintContext p) {
		print(getSubject(), p);
		p.print(" ");
		
		Statement pathS = getProperty(SP.path);
		if(pathS == null || pathS.getObject().isLiteral()) {
			p.print("<Missing path>");
		}
		else {
			Resource path = pathS.getResource();
			printPath(path, p);
		}
		
		p.print(" ");
		print(getObject(), p);
	}
	
	
	private void printPath(Resource path, PrintContext p) {
		Path arqPath = createPath(path);
		if(p.getUsePrefixes()) {
			PrefixMapping prefixMapping = path.getModel().getGraph().getPrefixMapping();
			String str = PathWriter.asString(arqPath, new Prologue(prefixMapping));
			p.print(str);
		}
		else {
			String str = PathWriter.asString(arqPath);
			p.print(str);
		}
	}
	
	
	private Path createPath(Resource path) {
		if(path.isURIResource()) {
			return new P_Link(path.asNode());
		}
		else {
			Statement typeS = path.getProperty(RDF.type);
			if(typeS != null && typeS.getObject().isURIResource()) {
				Resource type = typeS.getResource();
				if(SP.AltPath.equals(type)) {
					Path leftPath = createPath(path, SP.path1);
					Path rightPath = createPath(path, SP.path2);
					return new P_Alt(leftPath, rightPath);
				}
				else if(SP.ModPath.equals(type)) {
					Path subPath = createPath(path, SP.subPath);
					long min = path.getProperty(SP.modMin).getLong();
					long max = path.getProperty(SP.modMax).getLong();
					if(max < 0) {
						if(min == 1) {
							return new P_OneOrMore1(subPath);  // TODO: is this correct?
						}
						else if(max == -1) {
							return new P_ZeroOrOne(subPath);
						}
						else { // -2
							return new P_ZeroOrMore1(subPath);  // TODO: is this correct?
						}
					}
					else {
						return new P_Mod(subPath, min, max);
					}
				}
				else if(SP.ReversePath.equals(type)) {
					Path subPath = createPath(path, SP.subPath);
					return new P_Inverse(subPath);
				}
				else if(SP.SeqPath.equals(type)) {
					Path leftPath = createPath(path, SP.path1);
					Path rightPath = createPath(path, SP.path2);
					return new P_Seq(leftPath, rightPath);
				}
				else if(SP.ReverseLinkPath.equals(type)) {
					Node node = JenaUtil.getProperty(path, SP.node).asNode();
					return new P_ReverseLink(node);
				}
			}
			return null;
		}
	}

	
	private Path createPath(Resource subject, Property predicate) {
		Statement s = subject.getProperty(predicate);
		if(s != null && s.getObject().isResource()) {
			return createPath(s.getResource());
		}
		else {
			return null;
		}
	}	
}
