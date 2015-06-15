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
package fr.lirmm.graphik.graal.forward_chaining;


/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public interface Chase {

	/**
	 * Sature the fact base
	 */
	public void execute() throws ChaseException;;
	
	/**
	 * Execute the next step of the saturation process
	 * @throws ChaseException 
	 */
	public void next() throws ChaseException;
	
	public boolean hasNext();
	
}