package parsing.SplGeneratorModels;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Fragment extends SequenceDiagramElement{

	/**
	 * Constants defined for specifying the types of combined fragments
	 * 
	 */
	public static final int OPTIONAL = 1;
	public static final int ALTERNATIVE = 2;
	public static final int PARALLEL = 3;
	public static final int LOOP = 4;
	
	private int type; //for representing ALT, OPT or Loop fragments
	private String guard; //for representing guard conditions
	private LinkedList <SequenceDiagram> sequenceDiagrams; //each fragment is 
								// composed by, at least, one sequence diagram.
	
	
	public Fragment(String name) {
		super(name);
		sequenceDiagrams = new LinkedList<SequenceDiagram>();
	}


	/**
	 * This method sets the type of the fragment by the value passed to the 
	 * type parameter. A fragment can be optional, alternative, parallel or 
	 * loop.
	 * @param type The values for this parameter are defined by the constants
	 * OPTIONAL, ALTERNATIVE, PARALLEL or LOOP.
	 */
	public void setType(int type) {
		this.type = type; 
	}


	public int getType() {
		return type;
	}


	public SequenceDiagram addSequenceDiagram(String rowName, String guard) {
		String elementName = this.getName() + " - " + rowName; 
		SequenceDiagram rowSD = SequenceDiagram.createSequenceDiagram(
				elementName, guard); 
		boolean answer;
		boolean diagramsIsEmpty = sequenceDiagrams.isEmpty();
		switch (type) {
		case OPTIONAL:
			answer = addRowSD(diagramsIsEmpty, rowSD);
			break;

		case PARALLEL:
			answer = addRowSD(diagramsIsEmpty, rowSD);
			break;
			
		case ALTERNATIVE:
			answer = sequenceDiagrams.add(rowSD);
			break;
			
		case LOOP: 
			answer = addRowSD(diagramsIsEmpty, rowSD);
			break;
		default:
			answer = false; 
			break;
		}
		
		if (answer) 
			return rowSD; 
		else 
			return null; 
	}
	
	public boolean addRowSD(boolean diagramsIsEmpty, SequenceDiagram rowSD) {
		if (!diagramsIsEmpty) {
			sequenceDiagrams.clear();
		}
		return sequenceDiagrams.add(rowSD);
	}


	public LinkedList<SequenceDiagram> getSequenceDiagrams() {
		return sequenceDiagrams;
	}


	
	public Element getDOM(Document doc) {
		Element root = doc.createElement("Fragment");
		
		//Defining the attributes of FRAGMENT DOM element
		root.setAttribute("name", getName());
		switch (type) {
		case ALTERNATIVE:	
			root.setAttribute("type", "alternative");
			break;

		case LOOP: 
			root.setAttribute("type", "loop");
			break;
			
		case OPTIONAL: 
			root.setAttribute("type", "optional");
			break;
			
		case PARALLEL: 
			root.setAttribute("type", "parallel");
			break;
			
		default:
			break;
		}
		
		//Defining the REPRESENTEDBY DOM element
		
		Iterator<SequenceDiagram> it = sequenceDiagrams.iterator(); 
		while (it.hasNext()) {
			SequenceDiagram s = it.next();
			Element rb = doc.createElement("RepresentedBy"); 
			rb.setAttribute("seqDiagName", s.getName());
			root.appendChild(rb); 
		}
		
		return root;
	}


	public LinkedList<SequenceDiagram> getTransitiveSequenceDiagram() {
		LinkedList<SequenceDiagram> answer = new LinkedList<SequenceDiagram>();
		Iterator<SequenceDiagram> its = sequenceDiagrams.iterator(); 
		while (its.hasNext()) {
			SequenceDiagram s = its.next();
			answer.add(s);
			answer.addAll(s.getTransitiveSequenceDiagram());
		}
		
		return answer;
	}


	public HashSet<Lifeline> getTransitiveLifeline() {
		HashSet<Lifeline> answer = new HashSet<Lifeline>();
		//For each sequence diagram part of the fragment, we get all of 
		//its lifelines
		Iterator<SequenceDiagram> itSd = sequenceDiagrams.iterator(); 
		while (itSd.hasNext()) {
			SequenceDiagram s = itSd.next(); 
			answer.addAll(s.getTransitiveLifeline()); 
		}
		
		return answer;
	}


	public HashSet<Fragment> getTransitiveFragments() {
		HashSet<Fragment> answer = new HashSet<Fragment>(); 
		Iterator<SequenceDiagram> itsd = sequenceDiagrams.iterator(); 
		while (itsd.hasNext()) {
			SequenceDiagram s = itsd.next(); 
			answer.addAll(s.getTransitiveFragments());
		}
		return answer; 
	}


	public boolean addSequenceDiagram(SequenceDiagram sd) {
		return sequenceDiagrams.add(sd);
	}
}
