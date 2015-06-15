/* Graal v0.7.4
 * Copyright (c) 2014-2015 Inria Sophia Antipolis - Méditerranée / LIRMM (Université de Montpellier & CNRS)
 * All rights reserved.
 * This file is part of Graal <https://graphik-team.github.io/graal/>.
 *
 * Author(s): Clément SIPIETER
 *            Mélanie KÖNIG
 *            Swan ROCHER
 *            Jean-François BAGET
 *            Michel LECLÈRE
 *            Marie-Laure MUGNIER
 */
 /**
 * 
 */
package fr.lirmm.graphik.graal.core.term;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lirmm.graphik.util.URI;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
public class DefaultTermFactory implements TermFactory {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultTermFactory.class);

	private static final TermFactory INSTANCE = new DefaultTermFactory();

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	// /////////////////////////////////////////////////////////////////////////

	public static TermFactory instance() {
		return INSTANCE;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public Term createTerm(Term term) {
		if (term instanceof DefaultVariable) {
			return term;
		} else if (term instanceof DefaultConstant) {
			return term;
		} else if (term instanceof DefaultLiteral) {
			return term; // not immutable
		} else if (term instanceof Variable) {
			return new DefaultVariable((Variable) term);
		} else if (term instanceof Constant) {
			return new DefaultConstant((Constant) term);
		} else if (term instanceof Literal) {
			return new DefaultLiteral((Literal) term);
		} else {
			return null;
		}
	}

	@Override
	public Term createTerm(Object o, Term.Type type) {
		switch (type) {
		case VARIABLE:
			return this.createVariable(o.toString());
		case CONSTANT:
			return this.createConstant(o.toString());
		case LITERAL:
			return this.createLiteral(o);
		default:
			LOGGER.error("unknown term type");
			return null;
		}
	}

	@Override
	public Variable createVariable(Object identifier) {
		return new DefaultVariable(identifier);
	}

	@Override
	public Literal createLiteral(Object value) {
		return new DefaultLiteral(value);
	}

	@Override
	public Literal createLiteral(URI datatype, Object value) {
		return new DefaultLiteral(datatype, value);
	}

	@Override
	public Constant createConstant(Object identifier) {
		return new DefaultConstant(identifier);
	}

	// /////////////////////////////////////////////////////////////////////////
	// OBJECT OVERRIDE METHODS
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	// /////////////////////////////////////////////////////////////////////////

}