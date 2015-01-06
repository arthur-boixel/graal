package fr.lirmm.graphik.graal.homomorphism;

import java.util.Iterator;
import java.util.LinkedList;

import fr.lirmm.graphik.graal.core.Atom;
import fr.lirmm.graphik.graal.core.BuiltInPredicate;
import fr.lirmm.graphik.graal.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.core.DefaultConjunctiveQuery;
import fr.lirmm.graphik.graal.core.Substitution;
import fr.lirmm.graphik.graal.core.Term;
import fr.lirmm.graphik.graal.core.atomset.AtomSet;
import fr.lirmm.graphik.graal.core.atomset.LinkedListAtomSet;
import fr.lirmm.graphik.graal.core.atomset.ReadOnlyAtomSet;
import fr.lirmm.graphik.graal.core.stream.SubstitutionReader;


public class ComplexHomomorphism<Q extends ConjunctiveQuery, F extends ReadOnlyAtomSet> implements Homomorphism<Q,F> {

	private Homomorphism<ConjunctiveQuery,F> rawSolver;
	private LinkedList<Atom> builtInAtoms;

	public ComplexHomomorphism(Homomorphism<ConjunctiveQuery,F> rawSolver) {
		this.rawSolver = rawSolver;
	}

    public SubstitutionReader execute(Q q, F f) throws HomomorphismException {
	    AtomSet rawAtoms = new LinkedListAtomSet();
		this.builtInAtoms = new LinkedList<Atom>();
		for (Atom a : q) {
			if (a.getPredicate() instanceof BuiltInPredicate) {
				this.builtInAtoms.add(a);
			}
			else {
				rawAtoms.add(a);
			}
		}
		DefaultConjunctiveQuery rawQuery = new DefaultConjunctiveQuery(rawAtoms);
		rawQuery.setAnswerVariables(q.getAnswerVariables());
		return new BuiltInSubstitutionReader(this.rawSolver.execute(rawQuery,f));
	}

	protected class BuiltInSubstitutionReader implements SubstitutionReader {

		public BuiltInSubstitutionReader(SubstitutionReader reader) {
			this.rawReader = reader;
			this.next = this.computeNext();
			this.next();
		}

		@Override
    	public boolean hasNext() {
			return this.current != null;
		}

		@Override
    	public Substitution next() {
			Substitution res = this.current;
			this.current = next;
			if (this.current != null)
				this.next = computeNext();
			return res;
		}

		protected Substitution computeNext() {
			if (this.rawReader.hasNext()) {
				Substitution res = this.rawReader.next();
				if (check(res)) {
					return res;
				}
				else {
					return computeNext();
				}
			}
			else {
				return null;
			}
		}

		protected boolean check(Substitution s) {
			for (Atom a : builtInAtoms) {
				Atom a2 = s.getSubstitut(a);
				if (!((BuiltInPredicate)(a2.getPredicate())).evaluate(a2.getTerms().toArray(new Term[a2.getTerms().size()]))) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void remove() { }

		@Override
    	public Iterator<Substitution> iterator() { return this; }

    	public void close() { this.rawReader.close(); this.rawReader = null; }

		private Substitution next;
		private Substitution current;
		private SubstitutionReader rawReader;

	};
};

