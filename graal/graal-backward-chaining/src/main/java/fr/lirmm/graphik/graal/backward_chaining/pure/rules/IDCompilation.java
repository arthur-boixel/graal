package fr.lirmm.graphik.graal.backward_chaining.pure.rules;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.lirmm.graphik.graal.backward_chaining.pure.utils.IDCondition;
import fr.lirmm.graphik.graal.backward_chaining.pure.utils.Misc;
import fr.lirmm.graphik.graal.backward_chaining.pure.utils.TermPartition;
import fr.lirmm.graphik.graal.core.Atom;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.DefaultFreeVarGen;
import fr.lirmm.graphik.graal.core.DefaultRule;
import fr.lirmm.graphik.graal.core.Predicate;
import fr.lirmm.graphik.graal.core.Rule;
import fr.lirmm.graphik.graal.core.Substitution;
import fr.lirmm.graphik.graal.core.Term;
import fr.lirmm.graphik.graal.core.atomset.AtomSet;
import fr.lirmm.graphik.graal.core.ruleset.IndexedByBodyPredicatesRuleSet;

public class IDCompilation extends AbstractRulesCompilation {

	private static DefaultFreeVarGen varGen = new DefaultFreeVarGen("X"
			+ Integer.toString(IDCompilation.class.hashCode()));

	private TreeSet<Predicate> predicates = new TreeSet<Predicate>();

	// a matrix for store conditions ( p -> q : [q][p] )
	private Map<Predicate, TreeMap<Predicate, LinkedList<IDCondition>>> conditions;

	private LinkedList<Rule> saturation;

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	// /////////////////////////////////////////////////////////////////////////

	public IDCompilation(Iterable<Rule> ruleSet) {
		super();
		this.saturation = extractCompilable(ruleSet);

	}

	public IDCompilation(Iterable<Rule> ruleSet, Iterable<Rule> compilation) {
		super();
		this.saturation = extractCompilable(ruleSet);
		this.load(compilation);
	}

	// /////////////////////////////////////////////////////////////////////////
	// GETTERS / SETTERS
	// /////////////////////////////////////////////////////////////////////////

	public Iterable<Rule> getSaturation() {
		return this.saturation;
	}

	/**
	 * load a ruleSet
	 * 
	 * @param file
	 * @throws FileNotFoundException
	 */
	public void load(Iterable<Rule> saturation) {
		this.saturation = new LinkedList<Rule>();
		for (Rule r : saturation) {
			this.saturation.add(r);
		}
		this.createIDCondition();
	}

	// /////////////////////////////////////////////////////////////////////////
	// METHODS
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void compile() {
		if (this.getProfiler() != null) {
			this.getProfiler().start("Compilation total time");
		}
		this.computeSaturation();
		this.createIDCondition();
		if (this.getProfiler() != null) {
			this.getProfiler().stop("Compilation total time");
		}
	}

	public LinkedList<IDCondition> getConditions(Predicate predB,
			Predicate predH) {
		LinkedList<IDCondition> res = null;
		if (predB.equals(predH)) {
			res = new LinkedList<IDCondition>();
			ArrayList<Term> terms = new ArrayList<Term>(predB.getArity());
			for (int i = 0; i < predH.getArity(); i++) {
				terms.add(varGen.getFreeVar());
			}
			res.add(new IDCondition(terms, terms));
		} else {
			Map<Predicate, LinkedList<IDCondition>> condH = conditions
					.get(predH);

			if (condH != null) {
				res = condH.get(predB);
			}
		}
		if (res != null)
			return res;
		else
			return new LinkedList<IDCondition>();
	}

	/**
	 * Remove compilable rule from ruleset and return a List of compilable
	 * rules.
	 * 
	 * @param ruleset
	 * @return
	 */
	public LinkedList<Rule> extractCompilable(Iterable<Rule> ruleset) {
		LinkedList<Rule> compilable = new LinkedList<Rule>();
		Rule r;

		Iterator<Rule> it = ruleset.iterator();
		while (it.hasNext()) {
			r = it.next();
			if (this.isCompilable(r)) {
				compilable.add(r);
				it.remove();
			}
		}

		return compilable;
	}

	@Override
	public boolean isCompilable(Rule r) {
		Iterator<Atom> bodyIt = r.getBody().iterator();
		Iterator<Atom> headIt = r.getHead().iterator();

		if (bodyIt.hasNext() && headIt.hasNext()) {
			Atom b = bodyIt.next();
			Atom h = headIt.next();

			if (!bodyIt.hasNext() && !headIt.hasNext()) {
				// atomic head and body
				predicates.add(b.getPredicate());
				predicates.add(h.getPredicate());

				for (Term t : h.getTerms())
					if (t.isConstant() || r.getExistentials().contains(t))
						return false;
				return true;
			}
		}
		return false;
	}

	/**
	 * can return true if there are not mappable
	 */
	@Override
	public boolean isMappable(Atom father, Atom son) {
		Predicate predB = son.getPredicate();
		Predicate predH = father.getPredicate();
		if (predB.equals(predH))
			return true;

		else
			return !getConditions(predB, predH).isEmpty();
	}

	@Override
	public LinkedList<Substitution> getMapping(Atom father, Atom son) {
		LinkedList<Substitution> res = new LinkedList<Substitution>();
		Predicate predB = son.getPredicate();
		Predicate predH = father.getPredicate();
		LinkedList<IDCondition> conds = getConditions(predB, predH);
		for (IDCondition cond : conds) {
			if (cond.checkBody(son.getTerms()))
				res.add(cond.getSubstitution(son.getTerms(), father.getTerms()));
		}
		return res;
	}

	/**
	 * can return true if there are not unifiable
	 */
	@Override
	public boolean isUnifiable(Atom father, Atom son) {
		return isMappable(father, son);
	}

	@Override
	public LinkedList<TermPartition> getUnification(Atom father, Atom son) {
		LinkedList<TermPartition> res = new LinkedList<TermPartition>();
		Predicate predB = son.getPredicate();
		Predicate predH = father.getPredicate();
		LinkedList<IDCondition> conds = getConditions(predB, predH);
		for (IDCondition cond : conds) {
			res.add(cond.getUnification(son.getTerms(), father.getTerms()));
		}
		return res;
	}

	@Override
	public boolean isImplied(Atom father, Atom son) {
		Predicate predB = son.getPredicate();
		Predicate predH = father.getPredicate();
		LinkedList<IDCondition> conds = getConditions(predB, predH);
		for (IDCondition cond : conds)
			if (cond.check(son.getTerms(), father.getTerms()))
				return true;

		return false;
	}

	@Override
	public Collection<Predicate> getUnifiablePredicate(Predicate p) {
		Collection<Predicate> res = new LinkedList<Predicate>();
		Map<Predicate, LinkedList<IDCondition>> condH = conditions.get(p);
		res.add(p);
		if (condH != null)
			res.addAll(condH.keySet());

		return res;
	}

	/**
	 * Return all possible rewritings of this Atom by this compilation.
	 */
	@Override
	public Collection<Atom> getRewritingOf(Atom atom) {
		TreeSet<Atom> res = new TreeSet<Atom>();
		res.add(atom);

		Predicate predH = atom.getPredicate();
		Map<Predicate, LinkedList<IDCondition>> condH = this.conditions
				.get(predH);
		if (condH != null) {
			LinkedList<IDCondition> conds;
			Predicate predB;
			for (Map.Entry<Predicate, LinkedList<IDCondition>> entry : condH
					.entrySet()) {
				predB = entry.getKey();
				conds = entry.getValue();
				for (IDCondition cond : conds) {
					if (cond.checkHead(atom.getTerms()))
						res.add(new DefaultAtom(predB, cond.getBody(atom
								.getTerms())));
				}
			}
		}
		return res;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	// /////////////////////////////////////////////////////////////////////////

	private void createIDCondition() {
		if (this.getProfiler() != null) {
			this.getProfiler().start("Compilation create IDCondition time");
		}
		Atom b;
		Atom h;
		this.conditions = new TreeMap<Predicate, TreeMap<Predicate, LinkedList<IDCondition>>>();
		for (Rule ru : this.saturation) {
			h = ru.getHead().iterator().next();
			b = ru.getBody().iterator().next();
			this.addCondition(b.getPredicate(), h.getPredicate(), b.getTerms(),
					h.getTerms());
		}
		if (this.getProfiler() != null) {
			this.getProfiler().stop("Compilation create IDCondition time");
		}
	}

	private void computeSaturation() {
		// FIXME pb à résoudre avec unification trop importante

		if (this.getProfiler() != null) {
			this.getProfiler().start("Compilation saturation time");
		}
		IndexedByBodyPredicatesRuleSet rules = new IndexedByBodyPredicatesRuleSet();
		for (Rule r : saturation) {
			rules.add(Misc.getSafeCopy(r));
		}

		Collection<Rule> lastCompute = new LinkedList<Rule>();
		lastCompute.addAll(saturation);

		while (!lastCompute.isEmpty()) {
			lastCompute = computeSaturationPart(lastCompute, rules);
			saturation.addAll(lastCompute);
		}
		if (this.getProfiler() != null) {
			this.getProfiler().stop("Compilation saturation time");
		}
	}

	private Collection<Rule> computeSaturationPart(Iterable<Rule> lastCompute,
			IndexedByBodyPredicatesRuleSet rules) {
		Atom head1, body2;
		TermPartition part;
		AtomSet impliedHead, impliedBody;
		Rule impliedRule;

		LinkedList<Rule> tmp = new LinkedList<Rule>();

		for (Rule r1 : lastCompute) {
			head1 = r1.getHead().iterator().next();
			for (Rule r2 : rules.getRulesByBodyPredicate(head1.getPredicate())) {
				body2 = r2.getBody().iterator().next();
				if (head1.getPredicate().equals(body2.getPredicate())) {
					part = TermPartition.getPartitionByPosition(head1, body2);
					impliedBody = part.getAssociatedSubstitution(null)
							.getSubstitut(r1.getBody());
					impliedHead = part.getAssociatedSubstitution(null)
							.getSubstitut(r2.getHead());
					if (!impliedHead.equals(impliedBody)) {
						impliedRule = new DefaultRule(impliedBody, impliedHead);
						if (mustBeKeeped(impliedRule)) {
							tmp.add(impliedRule);
						}
					}
				}
			}
		}

		return tmp;
	}

	/**
	 * return true if saturation does not already contain a rule that implied
	 * the given one
	 */
	private boolean mustBeKeeped(Rule rule) {
		Iterator<Rule> it = saturation.iterator();
		Rule o;
		boolean isImplied = false;
		while (!isImplied && it.hasNext()) {
			o = it.next();
			if (Misc.imply(o, rule))
				isImplied = true;
		}
		return !isImplied;
	}

	private void addCondition(Predicate predB, Predicate predH, List<Term> b,
			List<Term> h) {

		Map<Predicate, LinkedList<IDCondition>> condH = this.conditions
				.get(predH);
		LinkedList<IDCondition> conds;
		if (condH != null) {
			conds = condH.get(predB);
			if (conds == null) {
				condH.put(predB, new LinkedList<IDCondition>());
				conds = condH.get(predB);
			}
		} else {
			conditions.put(predH,
					new TreeMap<Predicate, LinkedList<IDCondition>>());
			condH = conditions.get(predH);
			condH.put(predB, new LinkedList<IDCondition>());
			conds = condH.get(predB);
		}
		conds.add(new IDCondition(b, h));
	}

}
