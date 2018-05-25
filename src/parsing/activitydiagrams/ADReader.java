package parsing.activitydiagrams;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import parsing.ProbabilityEnergyTimeProfile;
import parsing.ProbabilityEnergyTimeProfileReader;
import parsing.exceptions.InvalidTagException;

/**
 * @author andlanna
 *
 */
public class ADReader {
    private static final Logger LOGGER = Logger.getLogger(ADReader.class.getName());

	private int index;
	private String name;
	private boolean next;
	private Map<String, Activity> activitiesByID;
	private Map<String, Edge> edgesByID;
	private Document doc;
	private List<Activity> activities;
	private List<Edge> edges;



	public ADReader(File xmlFile, int index) {
		this.index = index;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			this.doc = db.parse(xmlFile);
			this.doc.getDocumentElement().normalize();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}
	}

	public int getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}

	public List<Activity> getActivities() {
		return activities;
	}

	public void setActivities(List<Activity> activities) {
		this.activities = activities;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public void setEdges(List<Edge> edges) {
		this.edges = edges;
	}

	public boolean hasNext() {
		return next;
	}


	/**
	 * retrieveActivities function is responsible for identifying the activity diagrams
	 * represented in a XMI file, identify all the activities in each activity diagram and
	 * create the trace (link) between each activity and its related sequence diagram.
	 * @throws InvalidTagException
	 */
	public void retrieveActivities() throws InvalidTagException {
		//org.w3c.dom.Node ad;
		List<org.w3c.dom.Node> adList = new ArrayList<org.w3c.dom.Node>();
		NodeList nodes = this.doc.getElementsByTagName("packagedElement");
		updateList(adList, nodes);

		if ((this.index + 1) == adList.size())
			this.next = false;
		else
			this.next = true;

		org.w3c.dom.Node node = adList.get(this.index);
		this.name = node.getAttributes().getNamedItem("name").getTextContent();
		NodeList elements = node.getChildNodes();
		this.activities = new ArrayList<Activity>();

		activitiesAttributes(elements);
		resetActivitiesByID();
		retrieveEdges(node);
		solveActivities(node);
	}

	private void updateList(List<org.w3c.dom.Node> adList, NodeList nodes) {
		org.w3c.dom.Node ad;
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getAttributes().getNamedItem("xmi:type") != null) {
				String xmiType = nodes.item(i).getAttributes().getNamedItem("xmi:type")
						.getTextContent();
				if (xmiType != null && xmiType.equals("uml:Activity")) {
					ad = nodes.item(i);
					adList.add(ad);
				}
			}
		}
	}

	private void activitiesAttributes(NodeList elements) {
		for (int s = 0; s < elements.getLength(); s++) {
			if (elements.item(s).getNodeName().equals("node")) {
				Activity tmp;
				if (elements.item(s).getAttributes().getNamedItem("name") != null) {
					tmp = new Activity(elements.item(s).getAttributes().getNamedItem("xmi:id")
							.getTextContent(), elements.item(s).getAttributes()
							.getNamedItem("name").getTextContent(), elements.item(s)
							.getAttributes().getNamedItem("xmi:type").getTextContent());
				} else {
					tmp = new Activity(elements.item(s).getAttributes().getNamedItem("xmi:id")
							.getTextContent(), null, elements.item(s).getAttributes()
							.getNamedItem("xmi:type").getTextContent());
				}
				if (elements.item(s).getAttributes().getNamedItem("behavior") != null) {
					tmp.setSdID(elements.item(s).getAttributes().getNamedItem("behavior").getTextContent());
				} else {
					tmp.setSdID(null);
				}

				this.activities.add(tmp);
			}
		}
	}

	public void resetActivitiesByID() {
		this.activitiesByID = new HashMap<String, Activity>();
		for (Activity a : this.activities) {
			this.activitiesByID.put(a.getId(), a);
		}
	}



	/**
	 * The retrieveEdges functions is responsible for identifying all the edges between the activities
	 * represented at an activity diagram. Given an activity diagram the function traverse its XMI
	 * fragment and whenever it finds an edge node, the information needed is extract (id, edge name,
	 * edge type, source and target activities, guards conditions). By the end the function creates a
	 * mapping <id, edge>.
	 * @param node The XMI fragment representing a sequence diagram to be parsed.
	 * @throws InvalidTagException
	 */
	public void retrieveEdges(org.w3c.dom.Node node) throws InvalidTagException {
		NodeList elements = node.getChildNodes();
		this.edges = new ArrayList<Edge>();

		for (int s = 0; s < elements.getLength(); s++) {
			if (elements.item(s).getNodeName().equals("edge")) {
				Edge tmp;
				if (elements.item(s).getAttributes().getNamedItem("name") != null) {
					tmp = new Edge(elements.item(s).getAttributes().getNamedItem("xmi:id")
							.getTextContent(), elements.item(s).getAttributes()
							.getNamedItem("name").getTextContent(), elements.item(s)
							.getAttributes().getNamedItem("xmi:type").getTextContent());
				} else {
					tmp = new Edge(elements.item(s).getAttributes().getNamedItem("xmi:id")
							.getTextContent(), null, elements.item(s).getAttributes()
							.getNamedItem("xmi:type").getTextContent());
				}
				tmp.setSource(this.activitiesByID.get(elements.item(s).getAttributes()
						.getNamedItem("source").getTextContent()));
				tmp.setTarget(this.activitiesByID.get(elements.item(s).getAttributes()
						.getNamedItem("target").getTextContent()));

				NodeList infoNodes = elements.item(s).getChildNodes();
				nodeInfo(tmp, infoNodes);
				ProbabilityEnergyTimeProfile profile = ProbabilityEnergyTimeProfileReader.retrieveProbEnergyTime(tmp.getId(), this.doc);
				if (profile.hasProbability()) {
				    tmp.setProbability(profile.getProbability());
				}
				this.edges.add(tmp);
			}
		}
		resetEdgesByID();
	}

	private void nodeInfo(Edge tmp, NodeList infoNodes) {
		for (int i = 0; i < infoNodes.getLength(); i++) {
			if (infoNodes.item(i).getNodeName().equals("guard")) {
				NodeList guardNodes = infoNodes.item(i).getChildNodes();
				for (int j = 0; j < guardNodes.getLength(); j++) {
					if (guardNodes.item(j).getNodeName().equals("body")) {
						tmp.setGuard(guardNodes.item(j).getTextContent());
						break;
					}
				}
				break;
			}
		}
	}

	public void resetEdgesByID() {
		this.edgesByID = new HashMap<String, Edge>();
		for (Edge e : this.edges) {
			this.edgesByID.put(e.getId(), e);
		}
	}



	/**
	 * The solveActivities' role is to represent the activity diagram in memory (by using Activity
	 * and Edge objects. This function is called after retrieveActivities and retrieveEdges functions,
	 * because all the objects needed for creating the representation are avaiable. In the end the
	 * orderActivities function is called to ensure the order which the activities happen.
	 * @param node The node object (i.e. the XMI fragment) representing the whole activity diagram.
	 */
	public void solveActivities(org.w3c.dom.Node node) {
		NodeList elements = node.getChildNodes();
		for (int s = 0; s < elements.getLength(); s++) {
			if (elements.item(s).getNodeName().equals("node")) {
				Activity activity = this.activitiesByID.get(elements.item(s).getAttributes()
						.getNamedItem("xmi:id").getTextContent());
				NodeList tmpEdges = elements.item(s).getChildNodes();
				for (int t = 0; t < tmpEdges.getLength(); t++) {
					if (tmpEdges.item(t).getNodeName().equals("incoming")) {
						activity.addIncoming(this.edgesByID.get(tmpEdges.item(t).getAttributes()
								.getNamedItem("xmi:idref").getTextContent()));
					} else if (tmpEdges.item(t).getNodeName().equals("outgoing")) {
						activity.addOutgoing(this.edgesByID.get(tmpEdges.item(t).getAttributes()
								.getNamedItem("xmi:idref").getTextContent()));
					}
				}
			}
		}
		orderActivities();
	}




	/**
	 * This function is used to order the activities of a sequence diagram.
	 */
	public void orderActivities() {
		int i = 0, j;
		Queue<Activity> queue = new LinkedList<Activity>();
		Activity target, temp;

		j = -1;
		for (Activity a : this.activities) {
			j++;
			if (a.getType().equals(ActivityType.INITIAL_NODE)) {
				if (!this.activities.get(i).equals(a)) { // PEGAR EXEMPLO P
															// TESTAR AQUI
					temp = this.activities.get(i);
					this.activities.set(i, this.activities.get(j));
					this.activities.set(j, temp);
				}
				a.setOrdered(true);
				i++;
				queue.add(a);
				break;
			}
		}

		i = targetOrder(i, queue);
	}

	private int targetOrder(int i, Queue<Activity> queue) {
		int j;
		Activity target;
		Activity temp;
		while (!queue.isEmpty()) {
			for (Edge e : queue.element().getOutgoing()) {
				target = e.getTarget();
				if (!target.isOrdered()) { // nao esta ordenado
					if (!this.activities.get(i).equals(target)) { // ordem errada
						j = -1;
						for (Activity a : this.activities) {
							j++;
							if (a.equals(target)) { // j:posicao do target
													// i:posicao atual ordenacao
								temp = this.activities.get(i);
								this.activities.set(i, this.activities.get(j));
								this.activities.set(j, temp);
								break;
							}
						}
					}
					target.setOrdered(true); // marca ordem target certa
					i++;
					queue.add(target); // poe target na fila
				}
			}
			queue.poll(); // tira o primeiro da fila
		}
		return i;
	}
}
