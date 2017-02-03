/*
 * Copyright (C) Inria Sophia Antipolis - Méditerranée / LIRMM
 * (Université de Montpellier & CNRS) (2014 - 2017)
 *
 * Contributors :
 *
 * Clément SIPIETER <clement.sipieter@inria.fr>
 * Mélanie KÖNIG
 * Swan ROCHER
 * Jean-François BAGET
 * Michel LECLÈRE
 * Marie-Laure MUGNIER <mugnier@lirmm.fr>
 *
 *
 * This file is part of Graal <https://graphik-team.github.io/graal/>.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
 /**
 * 
 */
package fr.lirmm.graphik.graal.store.gdb;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.GraphQuery;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.VertexQuery;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSetException;
import fr.lirmm.graphik.graal.api.core.ConstantGenerator;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Term.Type;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.DefaultConstantGenerator;
import fr.lirmm.graphik.graal.core.store.GraphDBStore;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.util.MethodNotImplementedError;
import fr.lirmm.graphik.util.stream.AbstractCloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorAdapter;
import fr.lirmm.graphik.util.stream.IteratorException;

/**
 * BlueprintsGraphDBStore wrap  {@link <a href="http://blueprints.tinkerpop.com">Blueprints API</a>} into an AtomSet. Blueprints API allows you to
 * use many Graph Database like Neo4j, Sparksee, OrientDB, Titan...
 * 
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
public class BlueprintsGraphDBStore extends GraphDBStore {

	private final Graph graph;

	public BlueprintsGraphDBStore(Graph graph) {
		this.graph = graph;
		init();
	}

	private void init() {
		try {
			this.graph.getVertices("class", "");
		} catch (IllegalArgumentException e) {
			Vertex v = this.graph.addVertex(null);
			v.setProperty("class", "");
		}
	}

	@Override
	protected void finalize() throws Throwable {
		this.close();
		super.finalize();
	}

	// //////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// //////////////////////////////////////////////////////////////////////////

	@Override
	public void close() {
		this.graph.shutdown();
	}

	@Override
	public boolean contains(Atom atom) {
		GraphQuery query = this.graph.query();
		query.has("class", "atom");
		query.has("predicate", predicateToString(atom.getPredicate()));

		int i = 0;
		for (Term t : atom) {
			query.has("term" + i++, termToString(t));
		}

		return query.vertices().iterator().hasNext();
	}

	@Override
	public CloseableIterator<Atom> match(Atom atom) {
		GraphQuery query = this.graph.query();
		query.has("class", "atom");
		query.has("predicate", predicateToString(atom.getPredicate()));

		int i = -1;
		for (Term t : atom) {
			++i;
			if (t.isConstant()) {
				query.has("term" + i, termToString(t));
			}
		}

		return new AtomIterator(query.vertices().iterator());
	}

	@Override
	public CloseableIterator<Atom> atomsByPredicate(Predicate p) throws AtomSetException {
		GraphQuery query = this.graph.query();
		query.has("class", "atom");
		query.has("predicate", predicateToString(p));
		return new AtomIterator(query.vertices().iterator());
	}

	@Override
	public CloseableIterator<Term> termsByPredicatePosition(Predicate p, int position) throws AtomSetException {
		GraphQuery query = this.graph.query();
		query.has("class", "atom");
		query.has("predicate", predicateToString(p));
		Set<Term> terms = new TreeSet<Term>();
		AtomToTermIterator it = new AtomToTermIterator(query.vertices().iterator(), position);
		try {
			while (it.hasNext()) {
				terms.add(it.next());
			}
		} catch (IteratorException e) {
			throw new AtomSetException(e);
		} finally {
			it.close();
		}
		return new CloseableIteratorAdapter<Term>(terms.iterator());
	}

	@Override
	public CloseableIterator<Predicate> predicatesIterator() {
		return new PredicateIterator(graph.getVertices("class", "predicate").iterator());
	}

	@Override
	public Set<Predicate> getPredicates() throws AtomSetException {
		TreeSet<Predicate> set = new TreeSet<Predicate>();
		CloseableIterator<Predicate> it = this.predicatesIterator();
		try {
			while (it.hasNext()) {
				set.add(it.next());
			}
		} catch (IteratorException e) {
			throw new AtomSetException("An error occurs while iterating predicates", e);
		}
		return set;
	}

	@Override
	public Set<Term> getTerms() throws AtomSetException {
		Set<Term> terms = new TreeSet<Term>();
		for (Vertex v : this.graph.getVertices("class", "term")) {
			terms.add(vertexToTerm(v));
		}
		return terms;
	}

	@Override
	public CloseableIterator<Term> termsIterator() throws AtomSetException {
		return new CloseableIteratorAdapter<Term>(this.getTerms().iterator());
	}

	@Override
	@Deprecated
	public Set<Term> getTerms(Type type) throws AtomSetException {
		Set<Term> terms = new TreeSet<Term>();
		GraphQuery query = this.graph.query();
		query.has("class", "term");
		query.has("type", type.toString());

		for (Vertex v : query.vertices()) {
			terms.add(vertexToTerm(v));
		}
		return terms;
	}

	@Override
	@Deprecated
	public CloseableIterator<Term> termsIterator(Term.Type type) throws AtomSetException {
		return new CloseableIteratorAdapter<Term>(this.getTerms(type).iterator());
	}

	@Override
	public boolean add(Atom atom) {
		if (this.contains(atom)) {
			return false;
		}

		Vertex atomVertex = graph.addVertex(null);
		atomVertex.setProperty("class", "atom");
		atomVertex.setProperty("predicate",
				predicateToString(atom.getPredicate()));

		Vertex predicateVertex = this.add(atom.getPredicate());
		this.graph.addEdge(null, atomVertex, predicateVertex, "predicate");

		int i = 0;
		for (Term t : atom) {
			atomVertex.setProperty("term" + i, termToString(t));

			Vertex termVertex = this.add(t);
			Edge e = graph.addEdge(null, atomVertex, termVertex, "term");
			e.setProperty("index", i++);
		}

		return true;
	}

	private Vertex add(Predicate predicate) {
		Vertex v = null;

		GraphQuery query = this.graph.query();
		query.has("class", "predicate");
		query.has("value", predicate.getIdentifier());
		query.has("arity", predicate.getArity());
		Iterator<Vertex> it = query.vertices().iterator();

		if (it.hasNext()) {
			v = it.next();
		} else {
			v = graph.addVertex(null);
			v.setProperty("class", "predicate");
			v.setProperty("value", predicate.getIdentifier());
			v.setProperty("arity", predicate.getArity());
		}
		return v;
	}

	private Vertex add(Term term) {
		Vertex v = null;

		GraphQuery query = this.graph.query();
		query.has("class", "term");
		query.has("value", term.getIdentifier().toString());
		query.has("type", getType(term));
		Iterator<Vertex> it = query.vertices().iterator();

		if (it.hasNext()) {
			v = it.next();
		} else {
			v = this.graph.addVertex(null);
			v.setProperty("class", "term");
			v.setProperty("value", term.getIdentifier().toString());
			v.setProperty("type", getType(term));
		}
		return v;
	}

	@Override
	public boolean remove(Atom atom) {
		// TODO implement this method
		throw new MethodNotImplementedError();
	}

	@Override
	public void clear() {
		// TODO implement this method
		throw new MethodNotImplementedError();
	}

	@Override
	public CloseableIterator<Atom> iterator() {
		Iterator<Vertex> it = this.graph.getVertices("class", "atom").iterator();
		return new AtomIterator(it);
	}

	private ConstantGenerator freshSymbolGenerator = new DefaultConstantGenerator("EE");

	@Override
	public ConstantGenerator getFreshSymbolGenerator() {
		return freshSymbolGenerator;
	}

	// //////////////////////////////////////////////////////////////////////////
	// PRIVATE STATIC METHODS
	// //////////////////////////////////////////////////////////////////////////

	private static Predicate vertexToPredicate(Vertex vertex) {
		String label = vertex.getProperty("value");
		int arity = vertex.getProperty("arity");
		return new Predicate(label, arity);
	}

	private static Term vertexToTerm(Vertex vertex) throws AtomSetException {
		String type = vertex.getProperty("type").toString();
		if("V".equals(type)) {
			return DefaultTermFactory.instance().createVariable(vertex.getProperty("value"));
		} else if ("L".equals(type)) {
			return DefaultTermFactory.instance().createLiteral(vertex.getProperty("value"));
		} else if ("C".equals(type)) {
			return DefaultTermFactory.instance().createConstant(vertex.getProperty("value"));
		} else {			
			throw new AtomSetException("Unrecognized type: " + type);
		}
	
	}

	private static Atom vertexToAtom(Vertex vertex) throws AtomSetException {
		Iterator<Edge> it = vertex.getEdges(Direction.OUT, "predicate")
				.iterator();
		Vertex predicateVertex = it.next().getVertex(Direction.IN);
		Predicate p = vertexToPredicate(predicateVertex);

		Term[] terms = new Term[p.getArity()];

		for (Edge e : vertex.getEdges(Direction.OUT, "term")) {
			Vertex t = e.getVertex(Direction.IN);
			terms[(Integer) e.getProperty("index")] = vertexToTerm(t);
		}

		return new DefaultAtom(p, terms);
	}

	private static String predicateToString(Predicate p) {
		return p.getIdentifier() + "@" + p.getArity();
	}

	private static String termToString(Term t) {
		return t.getIdentifier().toString() + "@" + getType(t);
	}

	// //////////////////////////////////////////////////////////////////////////
	// PRIVATE CLASSES
	// //////////////////////////////////////////////////////////////////////////

	private static class AtomIterator extends AbstractCloseableIterator<Atom> {
		Iterator<Vertex> it;

		public AtomIterator(Iterator<Vertex> it) {
			this.it = it;
		}

		@Override
		public boolean hasNext() {
			return this.it.hasNext();
		}

		@Override
		public Atom next() throws IteratorException {
			try {
				return vertexToAtom(this.it.next());
			} catch (AtomSetException e) {
				throw new IteratorException(e);
			}
		}

		public void remove() {
			this.it.remove();
		}

		@Override
		public void close() {
		}
	}

	private static class PredicateIterator extends AbstractCloseableIterator<Predicate> {

		Iterator<Vertex> it;

		public PredicateIterator(Iterator<Vertex> iterator) {
			this.it = iterator;
		}

		@Override
		public boolean hasNext() {
			return this.it.hasNext();
		}

		@Override
		public Predicate next() {
			return vertexToPredicate(this.it.next());
		}

		public void remove() {
			this.it.remove();
		}

		@Override
		public void close() {
		}

	}

	private static class AtomToTermIterator extends AbstractCloseableIterator<Term> {

		Iterator<Vertex> it;
		int              position;

		public AtomToTermIterator(Iterator<Vertex> it, int position) {
			this.it = it;
			this.position = position;
		}

		@Override
		public boolean hasNext() {
			return this.it.hasNext();
		}

		@Override
		public Term next() throws IteratorException {
			VertexQuery query = this.it.next().query();
			query.has("index", position);
			try {
				return vertexToTerm(query.vertices().iterator().next());
			} catch (AtomSetException e) {
				throw new IteratorException(e);
			}
		}

		public void remove() {
			this.it.remove();
		}

		@Override
		public void close() {
		}

	}
	
	private static String getType(Term t) {
		if (t.isVariable()) {
			return "V";
		} else if (t.isLiteral()) {
			return "L";
		} else {
			return "C";
		}
	}


}
