package parsing.activitydiagrams;

import java.util.ArrayList;
import java.util.List;

import parsing.sequencediagrams.Fragment;

public class Activity {
	private String id;
	private String name;
	private ActivityType type;
	private List<Edge> incoming;
	private List<Edge> outgoing;
	private boolean ordered;
	private String sdID;
	private Fragment sd;

	public Activity(String id, String name, String type) {
		this.id = id;
		this.name = name;
		this.incoming = new ArrayList<Edge>();
		this.outgoing = new ArrayList<Edge>();
		this.ordered = false;
		activityType(type);
	}

	private void activityType(String type) {
		final String initial_node = "uml:InitialNode";
		final String final_node = "uml:ActivityFinalNode";
		final String call_behavior_action = "uml:CallBehaviorAction";
		final String decision_node = "uml:DecisionNode";
		final String merge_node = "uml:MergeNode";
		
		switch(type){
			case initial_node:
				this.type = ActivityType.INITIAL_NODE;
				break;
			case final_node:
				this.type = ActivityType.FINAL_NODE;
				break;
			case call_behavior_action:
				this.type = ActivityType.CALL;
				break;
			case decision_node:
				this.type = ActivityType.DECISION;
				break;
			case merge_node:
				this.type = ActivityType.MERGE;
				break;
			default:
				break;
		}

	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Edge> getIncoming() {
		return incoming;
	}

	public void setIncoming(List<Edge> incoming) {
		this.incoming = incoming;
	}

	public void addIncoming(Edge edge) {
		this.incoming.add(edge);
	}

    public List<Edge> getOutgoing() {
        return outgoing;
    }

    public int getOutgoingCount() {
        return outgoing.size();
    }

	public void setOutgoing(List<Edge> outgoing) {
		this.outgoing = outgoing;
	}

	public void addOutgoing(Edge edge) {
		this.outgoing.add(edge);
	}

	public String getId() {
		return id;
	}

	public ActivityType getType() {
		return type;
	}

	public boolean isOrdered() {
		return ordered;
	}

	public void setOrdered(boolean ordered) {
		this.ordered = ordered;
	}

	public String getSdID() {
		return sdID;
	}

	public void setSdID(String sdID) {
		this.sdID = sdID;
	}

	public Fragment getSd() {
		return sd;
	}

	public void setSd(Fragment sd) {
		this.sd = sd;
	}

	public String print() {
		String message = "Type: " + this.type + "; Name: " + this.name;
		if (this.sd != null) {
		    message += "; SD: " + this.sd.getName();
		}
		return message + "\n";
	}
}
