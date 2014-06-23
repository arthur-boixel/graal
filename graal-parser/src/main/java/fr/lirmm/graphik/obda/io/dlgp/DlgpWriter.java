/**
 * 
 */
package fr.lirmm.graphik.obda.io.dlgp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import fr.lirmm.graphik.kb.core.Atom;
import fr.lirmm.graphik.kb.core.Predicate;
import fr.lirmm.graphik.kb.core.ReadOnlyAtomSet;
import fr.lirmm.graphik.kb.core.Rule;
import fr.lirmm.graphik.kb.core.Term;
import fr.lirmm.graphik.kb.core.Term.Type;
import fr.lirmm.graphik.obda.writer.AtomWriter;

/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public class DlgpWriter extends Writer implements AtomWriter {

	private Writer writer;

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	// /////////////////////////////////////////////////////////////////////////

	public DlgpWriter() {
		this.writer = new OutputStreamWriter(System.out);
	}
	
	public DlgpWriter(OutputStream out) {
		this.writer = new OutputStreamWriter(out);
	}
	
	public DlgpWriter(Writer out) {
		this.writer = out;
	}
	
	public DlgpWriter(File file) throws IOException {
		this(new FileWriter(file));
	}
	
	public DlgpWriter(String path) throws IOException {
		 this(new FileWriter(path));
	}
	
	// /////////////////////////////////////////////////////////////////////////
	// METHODS
	// /////////////////////////////////////////////////////////////////////////
	
	@Override
	public void write(Atom atom) throws IOException{
		this.writeAtom(atom);
		this.writer.write(".\n");
		this.writer.flush();
	}
	
	@Override
	public void write(ReadOnlyAtomSet atomSet) throws IOException {
		this.writeAtomSet(atomSet, true);
		this.writer.write(".\n");
		this.writer.flush();
	}
	
	@Override
	public void write(Iterable<Atom> atoms) throws IOException {
		for(Atom a : atoms)
			this.writeAtom(a);
		this.writer.flush();
	}
	
	public void write(Rule rule) throws IOException {
		String label = rule.getLabel();
		if(!label.isEmpty()) {
			this.write("[");
			this.write(label);
			this.write("] ");
		}
		this.writeAtomSet(rule.getHead(), false);
		this.writer.write(" :- ");
		this.writeAtomSet(rule.getBody(), false);
		this.writer.write(".\n");
		this.writer.flush();
	}
	
	// /////////////////////////////////////////////////////////////////////////
	// OVERRIDE METHODS
	// /////////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see java.io.Writer#close()
	 */
	@Override
	public void close() throws IOException {
		this.writer.close();
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#flush()
	 */
	@Override
	public void flush() throws IOException {
		this.writer.flush();
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#write(char[], int, int)
	 */
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		this.writer.write(cbuf, off, len);
	}
	
	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	// /////////////////////////////////////////////////////////////////////////
	
	private void writeAtomSet(ReadOnlyAtomSet atomSet, boolean addCarriageReturn) throws IOException {
		boolean isFirst = true;
		for(Atom a : atomSet) {
			if(isFirst) {
				isFirst = false;
			} else {
				this.writer.write(", ");
				if(addCarriageReturn)
					this.writer.write('\n');
			}
			
			this.writeAtom(a);
		}
	}
	
	private void writeAtom(Atom atom) throws IOException {
		this.writePredicate(atom.getPredicate());
		this.writer.write('(');
		
		boolean isFirst = true;
		for(Term t : atom.getTerms()) {
			String term = t.toString();
			if(isFirst) {
				isFirst = false;
			} else {
				this.writer.write(", ");
			}
			
			if(Type.VARIABLE.equals(t.getType())) {
				if (term.charAt(0) < 65 || term.charAt(0) > 90) {
					this.writer.write("VAR_");
					this.writer.write(term);
				}
			} else if(Type.CONSTANT.equals(t.getType())) {
				if (term.charAt(0) < 97 || term.charAt(0) > 122) {
					this.writer.write("cst_");
					this.writer.write(term);
				}
			} else {
				this.writer.write(t.toString());
			}
		}
		this.writer.write(')');
	}
	
	private void writePredicate(Predicate p) throws IOException {
		String s = p.getLabel();
		if(s.charAt(0) != '"') {
			this.writer.write('"');
		}

		this.writer.write(s);
		if(s.charAt(0) != '"') {
			this.writer.write('"');
		}
	}
	
};