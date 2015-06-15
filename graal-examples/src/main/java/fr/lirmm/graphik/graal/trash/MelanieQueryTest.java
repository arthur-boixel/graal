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
 package fr.lirmm.graphik.graal.trash;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.lirmm.graphik.graal.core.Atom;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.Predicate;
import fr.lirmm.graphik.graal.core.Query;
import fr.lirmm.graphik.graal.core.Substitution;
import fr.lirmm.graphik.graal.core.UnionConjunctiveQueries;
import fr.lirmm.graphik.graal.core.atomset.AtomSet;
import fr.lirmm.graphik.graal.core.atomset.AtomSetException;
import fr.lirmm.graphik.graal.core.stream.SubstitutionReader;
import fr.lirmm.graphik.graal.core.stream.filter.AtomFilterIterator;
import fr.lirmm.graphik.graal.forward_chaining.Chase;
import fr.lirmm.graphik.graal.forward_chaining.ChaseException;
import fr.lirmm.graphik.graal.forward_chaining.ChaseWithGRDAndUnfiers;
import fr.lirmm.graphik.graal.grd.GraphOfRuleDependencies;
import fr.lirmm.graphik.graal.homomorphism.HomomorphismException;
import fr.lirmm.graphik.graal.homomorphism.HomomorphismFactoryException;
import fr.lirmm.graphik.graal.homomorphism.StaticHomomorphism;
import fr.lirmm.graphik.graal.io.ParseException;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;
import fr.lirmm.graphik.graal.io.dlp.DlgpWriter;
import fr.lirmm.graphik.graal.io.grd.GRDParser;
import fr.lirmm.graphik.graal.io.oxford.OxfordQueryParser;
import fr.lirmm.graphik.graal.store.rdbms.DefaultRdbmsStore;
import fr.lirmm.graphik.graal.store.rdbms.driver.DriverException;
import fr.lirmm.graphik.graal.store.rdbms.driver.MysqlDriver;
import fr.lirmm.graphik.graal.store.rdbms.driver.SqliteDriver;

/**
 * 
 */

/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public class MelanieQueryTest {
	
	public static final String GRD_FILE = "./src/test/resources/u/u.grd";
	public static final String FACT_FILE = "./src/test/resources/u/University0_0.dlp";
	
	private static List<Query> queries = new LinkedList<Query>(); 
	
	public static void main(String[] args) throws AtomSetException, IOException, ChaseException, HomomorphismFactoryException, HomomorphismException, ParseException {
		
		
		AtomSet atomSet = getMysqlAtomSet(true, "univ_bench");
		//addFacts(atomSet);
		//forwardChaining(atomSet);
		

		parseUnionQuery();
		long time, time2;
		for(Query query : queries) { 
			System.out.println(query);
			time = System.currentTimeMillis();
			SubstitutionReader subR = StaticHomomorphism.executeQuery(query, atomSet);
			time2 = System.currentTimeMillis();
			
			int i = 0;
			for(Iterator<Substitution> it = subR.iterator(); it.hasNext(); it.next())
				++i;
			
			System.out.println(i + " results in " + (time2 - time) + "ms");
		}
			
	}
		

	public static void forwardChaining(AtomSet atomSet) throws FileNotFoundException, IOException, ChaseException, ParseException {
		GraphOfRuleDependencies grd = GRDParser.getInstance().parse(
				new File(GRD_FILE));

		
		Chase chase = new ChaseWithGRDAndUnfiers(grd, atomSet);
		
		System.out.println("forward chaining");
		long time = System.currentTimeMillis();
		chase.execute();
		long time2 = System.currentTimeMillis();
		System.out.println("Forward chaining time: " + (time2 - time) );
		
	}

	
	public static void addFacts(AtomSet atomset) throws FileNotFoundException, AtomSetException {
		//RDFParser rdfParser = new RDFParser(new FileReader("./src/test/resources/u/University0_0.owl"));
		DlgpParser parser = new DlgpParser(new FileReader(FACT_FILE));
		
		//atomSet.add(new RDFPrefixFilter(new RDF2Atom(rdfParser),"http://swat.cse.lehigh.edu/onto/univ-bench.owl#"));
		atomset.addAll(new AtomFilterIterator(parser));
	}
	
	public static AtomSet getSqliteAtomSet(boolean deleteIfExist, String base ) throws IOException, AtomSetException, DriverException {
		File f = new File(base);
		if(deleteIfExist) {
			f.delete();
			f.createNewFile();
		}
		return  new DefaultRdbmsStore(new SqliteDriver(f));
	}
	
	public static AtomSet getMysqlAtomSet(boolean deleteIfExist, String base) throws IOException, AtomSetException {
		
		return  new DefaultRdbmsStore(new MysqlDriver("localhost", base, "root", "root"));
	}
		
	
	public static void parseDefaultQuery() throws ParseException {
		queries.add(OxfordQueryParser.parseQuery("Q(?0) <- worksFor(?0,?1), affiliatedOrganizationOf(?1,?2)"));
		queries.add(OxfordQueryParser.parseQuery("Q(?0,?1) <- Person(?0), teacherOf(?0,?1), Course(?1)"));
		queries.add(OxfordQueryParser.parseQuery("Q(?0,?1,?2) <- Student(?0), advisor(?0,?1), FacultyStaff(?1), takesCourse(?0,?2), teacherOf(?1,?2), Course(?2)"));
		queries.add(OxfordQueryParser.parseQuery("Q(?0,?1) <- Person(?0), worksFor(?0,?1), Organization(?1)"));
		queries.add(OxfordQueryParser.parseQuery("Q(?0) <- Person(?0), worksFor(?0,?1), University(?1), hasAlumnus(?1,?0)"));

		queries.add(DlgpParser.parseQuery("?(X) :- \"Student\"(X)."));
		queries.add(DlgpParser.parseQuery("?(X) :- \"Course\"(X)."));
		queries.add(DlgpParser.parseQuery("?(X,Y) :- \"advisor\"(X,Y)."));
		queries.add(DlgpParser.parseQuery("?(X) :- \"FacultyStaff\"(X)."));
		queries.add(DlgpParser.parseQuery("?(X,Y) :- \"takesCourse\"(X,Y)."));
		queries.add(DlgpParser.parseQuery("?(X,Y) :- \"teacherOf\"(X,Y)."));

	}
	

	public static void parseMelanieQuery() throws ParseException {
		
		queries.add(OxfordQueryParser.parseQuery("Q(?0) <- worksfor(?0,?1), affiliatedorganizationof(?1,?2)"));
		queries.add(OxfordQueryParser.parseQuery("Q(?0,?1) <- teacherOf(?0,?1)"));
		queries.add(OxfordQueryParser.parseQuery("Q(?0,?1,?2) <- student(?0), advisor(?0,?1), takescourse(?0,?2), teacherof(?1,?2)"));
		queries.add(OxfordQueryParser.parseQuery("Q(?0,?1) <-  worksfor(?0,?1)"));
		queries.add(OxfordQueryParser.parseQuery("Q(?0) <-  worksfor(?0,?1), hasalumnus(?1,?0)"));

	}
	
	
	public static void parseUnionQuery() throws ParseException {

		UnionConjunctiveQueries ucq = new UnionConjunctiveQueries();
		ucq.add(DlgpParser.parseQuery("?(A,B) :- affiliatedOrganizationOf(B,C), headOf(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- affiliatedOrganizationOf(B,C), worksFor(A,B)."));
		queries.add(ucq);

		ucq = new UnionConjunctiveQueries();
		ucq.add(DlgpParser.parseQuery("?(A,B) :- teacherOf(A,B)."));
		queries.add(ucq);
		
		ucq = new UnionConjunctiveQueries();
		ucq.add(DlgpParser.parseQuery("?(A,B,C) :- hasExamrecord(A,D), takesCourse(A,C), teacherOf(B,C)." ));
		ucq.add(DlgpParser.parseQuery("?(A,B,C) :- advisor(A,B), \"ResearchAssistant\"(A), takesCourse(A,C), teacherOf(B,C)." ));
		ucq.add(DlgpParser.parseQuery("?(A,B,C) :- advisor(A,B), \"Student\"(A), takesCourse(A,C), teacherOf(B,C)." ));
		ucq.add(DlgpParser.parseQuery("?(A,B,C) :- takesCourse(A,C), teacherOf(B,C), \"UndergraduateStudent\"(A)." ));
		queries.add(ucq);

		ucq = new UnionConjunctiveQueries();
		ucq.add(DlgpParser.parseQuery("?(A,B) :- headOf(A,B)." ));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- worksFor(A,B)." ));
		queries.add(ucq);

		ucq = new UnionConjunctiveQueries();
		ucq.add(DlgpParser.parseQuery("?(A,B) :- degreeFrom(A,B), headof(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- degreeFrom(A,B), worksfor(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- doctoralDegreeFrom(A,B), headOf(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- doctoralDegreeFrom(A,B), worksFor(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- hasAlumnus(B,A), headOf(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- hasAlumnus(B,A), worksFor(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- headOf(A,B), mastersDegreeFrom(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- mastersDegreeFrom(A,B), worksfor(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- headOf(A,B), undergraduateDegreefrom(A,B)."));
		ucq.add(DlgpParser.parseQuery("?(A,B) :- undergraduateDegreeFrom(A,B), worksFor(A,B)."));
		queries.add(ucq);

	}
	
	
	
	public static void lowerCaseFact() throws AtomSetException, IOException {
		DlgpParser parser = new DlgpParser(new FileReader("./src/test/resources/u/University0_0.dlp"));
		DlgpWriter writer = new DlgpWriter(new File("./src/test/resources/u/University0_0_lowercase.dlp"));
		for(Object o: parser) {
			if(o instanceof Atom) {
				Atom atom = (Atom)o;
				Predicate p = new Predicate(atom.getPredicate().toString().toLowerCase(), atom.getPredicate().getArity());
				Atom a = new DefaultAtom(p, atom.getTerms());
				writer.write(a);
			}
		}
		writer.close();
	}
}