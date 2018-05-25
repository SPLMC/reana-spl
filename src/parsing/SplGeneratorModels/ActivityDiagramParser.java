package parsing.SplGeneratorModels;

import java.util.HashSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import parsing.SplGeneratorModels.Activity;
import parsing.SplGeneratorModels.ActivityDiagram;
import parsing.SplGeneratorModels.ActivityDiagramElement;
import parsing.SplGeneratorModels.SequenceDiagram;
import parsing.SplGeneratorModels.StartNode;
import parsing.SplGeneratorModels.Transition;

public class ActivityDiagramParser {

	private ActivityDiagram instance; 
	
	private static String activityDiagramName; 
	private static HashSet<ActivityDiagramElement> elements; 
	private static HashSet<Transition> transitions; 
	
	public static ActivityDiagram parse(Document doc) {
		ActivityDiagram answer = new ActivityDiagram(); 
		elements = new HashSet<ActivityDiagramElement>();
		transitions = new HashSet<Transition>(); 
		 
		NodeList nNode = doc.getElementsByTagName("ActivityDiagram");
		
		for (int i = 0; i<nNode.getLength(); i++) {
			//for each activity diagram found, we should get its name, parse its
			//elements and transitions and rebuild it in memory. 
			Node nActDiag = nNode.item(i);
			
			//extracting the name of the activity diagram
			NamedNodeMap actDiagAttributes = nActDiag.getAttributes();
			Node actDiagName = actDiagAttributes.getNamedItem("name");
			activityDiagramName = actDiagName.getNodeValue();
			answer.setName(activityDiagramName);
			
			//parse the document seeking for <ActivityDiagramElement> and 
			//<Transition> elements. Later, ActivityDiagramElement and 
			//Transition objects will be created for representing them.
			NodeList setOfElements;
			NodeList setOfTransitions;
			
			Element e = (Element) nActDiag; 
			setOfElements = e.getElementsByTagName("ActivityDiagramElement"); 
			setOfTransitions = e.getElementsByTagName("Transition"); 
			
			for (int j = 0; j<setOfElements.getLength(); j++) {
				ActivityDiagramElement ade = null;
				
				Element el = (Element) setOfElements.item(j);
				NamedNodeMap map = el.getAttributes(); 
				String elementName = map.getNamedItem("name").getNodeValue();
				
				switch (map.getNamedItem("type").getNodeValue()) {
				case "Activity":
					ade = ActivityDiagramElement.createElement(ActivityDiagramElement.ACTIVITY, 
							elementName);
					if (el.hasChildNodes()) {
						answer = parseSequenceDiagrams (answer, el, ade);
						 //answer.addElement(ade); 
					}
					break;
				
				case "DecisionNode":
					ade = ActivityDiagramElement.createElement(ActivityDiagramElement.DECISION_NODE, 
							elementName); 
					answer.addElement(ade); 
					break;
				
				case "EndNode":
					ade = ActivityDiagramElement.createElement(ActivityDiagramElement.END_NODE, 
							elementName);
					answer.addElement(ade);
					break;
				
				case "MergeNode":
					ade = ActivityDiagramElement.createElement(ActivityDiagramElement.MERGE_NODE, 
							elementName);
					answer.addElement(ade);
					break;
				
				case "StartNode":
					StartNode sn = (StartNode)answer.getStartNode();
					sn.setElementName(elementName);
					break;
				
				case "Transition":
					ade = ActivityDiagramElement.createElement(ActivityDiagramElement.TRANSITION, 
							elementName);
					answer.addElement(ade);
					break;

				default:
					System.out.println(map.getNamedItem("type").getNodeValue() + 
							" not yet implemented! ");
					break;
				}
				elements.add(ade);
			}
			
			answer = parseTransitions(answer, setOfTransitions);
		}
		return answer;
	}
	
	public static ActivityDiagram parseSequenceDiagrams(ActivityDiagram answer,
												Element el, ActivityDiagramElement ade){
			//parse the sequence diagrams associated with it
			NodeList children = el.getElementsByTagName("RepresentedBy"); 
			 for (int k=0; k<children.getLength(); k++) {
				 if (children.item(k).getNodeType() == Node.ELEMENT_NODE && 
						 children.item(k).getNodeName().equals("RepresentedBy")){
					 Element repBy = (Element) children.item(k); 
					 NamedNodeMap attributes = repBy.getAttributes(); 
					 String seqDiagName = attributes.getNamedItem("seqDiagName").getNodeValue(); 
					 SequenceDiagram sd = SequenceDiagram.getSequenceDiagramByName(seqDiagName);
					 Activity a = (Activity) ade; 
					 a.addSequenceDiagram(sd); 
				 }
			 }
			 answer.addElement(ade); 
		
		return answer;
	}
	
	public static ActivityDiagram parseTransitions(ActivityDiagram answer, 
				 										NodeList setOfTransitions){
		for (int j=0; j<setOfTransitions.getLength(); j++) {
			Element f = (Element) setOfTransitions.item(j);
			NamedNodeMap map = f.getAttributes(); 
			
			String transitionName = map.getNamedItem("name").getNodeValue();
			double transitionProbability = Double.parseDouble(
					map.getNamedItem("probability").getNodeValue()); 
			String sourceElement = map.getNamedItem("source").getNodeValue(); 
			String targetElement = map.getNamedItem("target").getNodeValue();
			
			ActivityDiagramElement source = answer.getElementByName(sourceElement);
			ActivityDiagramElement target = answer.getElementByName(targetElement);
			Transition t = source.createTransition(target, transitionName, transitionProbability);
			answer.addElement(t);
		}
		return answer;
	}
	
	

}
