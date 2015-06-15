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
package fr.lirmm.graphik.graal.homomorphism;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lirmm.graphik.graal.core.Atom;
import fr.lirmm.graphik.graal.core.atomset.AtomSet;
import fr.lirmm.graphik.graal.core.stream.SubstitutionReader;
import fr.lirmm.graphik.graal.core.term.Term;

/**
 * A simple implementation of an algorithm to find if there exist an
 * homomorphism between two facts Backtrack algorithm that look for an
 * association of atoms that correspond to a substitution of terms efficient for
 * simple facts of small size
 * 
 * @author Mélanie KÖNIG
 * 
 */
public class PureHomomorphism implements
		Homomorphism<AtomSet, AtomSet> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(PureHomomorphism.class);

	private static PureHomomorphism instance;

	protected PureHomomorphism() {
	}

	public static synchronized PureHomomorphism getInstance() {
		if (instance == null)
			instance = new PureHomomorphism();

		return instance;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public SubstitutionReader execute(AtomSet source,
			AtomSet target) throws HomomorphismException {
		return null;
	}

	/**
	 * return true iff exist an homomorphism from the query to the fact else
	 * return false
	 */
	public boolean exist(AtomSet source, AtomSet target)
			throws HomomorphismException {

		Homomorphism homomorphism = new Homomorphism();

		// check if the query is empty
		if (source == null || !source.iterator().hasNext()) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Empty query");
			}
			return true;
		}

		// /////////////////////////////////////////////////////////////////////
		// Initialisation
		if (!initialiseHomomorphism(homomorphism, source, target))
			return false;

		return backtrack(homomorphism);
	}

	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * Initialise attribute for homomorphism
	 */
	protected boolean initialiseHomomorphism(Homomorphism homomorphism,
			Iterable<Atom> source, Iterable<Atom> target) {
		homomorphism.sourceAtoms = new ArrayList<Atom>();
		homomorphism.firstOccurence = computeFirstOccurence(source,
				homomorphism.sourceAtoms);

		int size = homomorphism.sourceAtoms.size();
		homomorphism.availableImage = new ArrayList<LinkedList<Atom>>(size);
		homomorphism.currentImages = new ArrayList<Integer>(size);
		for (int i = 0; i < size; ++i) {
			homomorphism.currentImages.add(-1);
			homomorphism.availableImage.add(null);
		}
		return initialiseAvailableImage(homomorphism, target);
	}

	protected static boolean backtrack(Homomorphism homomorphism) {
		int level = 0;
		boolean foundImage;
		boolean hasNextImage;

		// can backtrack and all the atom have not been associate
		while (level >= 0 && level < homomorphism.sourceAtoms.size()) {
			hasNextImage = chooseNextImage(level, homomorphism);
			if (hasNextImage) {// try next image
				foundImage = checkCurrentImage(level, homomorphism);
				if (foundImage) {// need go to the next atom
					level++;
				}
			} else { // need backtrack
				level--;
			}
		}
		return !(level < 0);
	}

	/**
	 * Compute the associations between an atom and the terms that first occur
	 * in this atom i.e. for which the substitution must be reset when the image
	 * of the atom change
	 * 
	 * @param sourceAtoms2
	 */
	protected static ArrayList<LinkedList<Term>> computeFirstOccurence(
			Iterable<Atom> source, ArrayList<Atom> sourceAtoms) {
		ArrayList<LinkedList<Term>> firstOccurenceArray = new ArrayList<LinkedList<Term>>();
		Set<Term> alreadySeen = new TreeSet<Term>();
		LinkedList<Term> firstOccurence;

		for (Atom a : source) {
			sourceAtoms.add(a);
			firstOccurence = new LinkedList<Term>();
			firstOccurenceArray.add(firstOccurence);
			for (Term t : a.getTerms()) {
				// if this is the first occurence of this term
				if (alreadySeen.add(t)) {
					// note that its affectation come from this atom
					firstOccurence.add(t);
				}
			}
		}

		return firstOccurenceArray;
	}

	/**
	 * return the current image of the given atom
	 */
	protected static Atom getImage(int level, Homomorphism homomorphism) {
		Integer numCurrentImage = homomorphism.currentImages.get(level);
		LinkedList<Atom> images = homomorphism.availableImage.get(level);
		return images.get(numCurrentImage);
	}

	/**
	 * Return true if the current image of the given atom is possible and
	 * compute the current substitution else return false
	 */
	protected static boolean checkCurrentImage(int level,
			Homomorphism homomorphism) {
		Atom atom = homomorphism.sourceAtoms.get(level);
		Atom image = getImage(level, homomorphism);
		List<Term> sourceTerms = atom.getTerms();
		List<Term> targetTerms = image.getTerms();
		for (int i = 0; i < atom.getPredicate().getArity(); i++) {
			Term currentImage = homomorphism.currentSubstitution
					.get(sourceTerms.get(i));
			// if the current image is no null and different of the target term
			// or if the source term is a constant différent than the target one
			if ((currentImage != null && !currentImage.equals(targetTerms
					.get(i)))
					|| (sourceTerms.get(i).isConstant() && !sourceTerms.get(i)
							.equals(targetTerms.get(i)))) { // incompatible
															// image
				resetSubstitution(level, homomorphism);
				return false;
			} else { // possible image
				homomorphism.currentSubstitution.put(sourceTerms.get(i),
						targetTerms.get(i));
			}
		}
		return true;
	}

	/**
	 * Reset substitution for term that first occur in the given atom Check if
	 * the given atom has a next image available if it has : increment
	 * currentImage for the given atom and return true if has not : reset
	 * current image for the given atom and return false
	 */
	protected static boolean chooseNextImage(int level, Homomorphism homomorphism) {
		Integer numCurrentImage = homomorphism.currentImages.get(level);
		if (numCurrentImage != null) {
			resetSubstitution(level, homomorphism);
			numCurrentImage++;
		} else {
			numCurrentImage = 0;
		}
		// if there is a next image in the available image
		if (numCurrentImage < homomorphism.availableImage.get(level).size()) {
			homomorphism.currentImages.set(level, numCurrentImage);
			return true;
		} else {
			homomorphism.currentImages.set(level, -1);
			return false;
		}
	}

	/**
	 * reset the substitution of terms that need when the given atom change it
	 * image
	 * 
	 * @param atom
	 */
	protected static void resetSubstitution(int level, Homomorphism homomorphism) {
		for (Term t : homomorphism.firstOccurence.get(level)) {
			homomorphism.currentSubstitution.put(t, null);
		}
	}

	/**
	 * Found the possible image of each atom in the source fact into the atoms
	 * of the target fact
	 */
	protected boolean initialiseAvailableImage(Homomorphism homomorphism,
			Iterable<Atom> target) {
		LinkedList<Atom> images;
		for (int i = 0; i < homomorphism.sourceAtoms.size(); ++i) {
			images = new LinkedList<Atom>();
			for (Atom im : target) {
				if (isMappable(homomorphism.sourceAtoms.get(i), im, homomorphism)) {
					images.add(im);
				}
			}
			if (images.isEmpty())
				return false;
			homomorphism.availableImage.set(i, images);
		}
		return true;
	}

	protected boolean isMappable(Atom a, Atom im, Homomorphism homomorphism) {
		return a.getPredicate().equals(im.getPredicate());
	}

	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE CLASS
	// /////////////////////////////////////////////////////////////////////////

	protected static class Homomorphism {

		// attribute for homomorphism computation
		ArrayList<Atom> sourceAtoms;
		ArrayList<LinkedList<Term>> firstOccurence;
		ArrayList<LinkedList<Atom>> availableImage;
		ArrayList<Integer> currentImages;
		Map<Term, Term> currentSubstitution = new TreeMap<Term, Term>();

	}

}