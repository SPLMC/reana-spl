package parsing.sequencediagrams;

import parsing.Node;

public class PrintInSequence {
	private final SDUtil _sdutil;
	private Fragment fragment;
	private String indent;
	private String message;
	
	PrintInSequence (SDUtil source, Fragment fragmentArg, String indentArg) {
		_sdutil = source;
		fragment = fragmentArg;
		indent = indent;
	}
	
	private String printMessages() {
	    String message = printLifelines();
	    message += printLifelines();
	    
	    return message;
	}
	
	public String printLifelines () {
		String lifeline = indent;
		lifeline += "Lifelines:\n";
		for (Lifeline l: fragment.getLifelines()) {
		    lifeline += indent;
		    lifeline += l.getName() + "\n";
		}
		lifeline += "\n";
		
		return lifeline;
	}
	
	public String printFragments () {
		String fragments = indent;
		fragments += "Nodes:\n";
		for (Node n: fragment.getNodes()) {
		    fragments += indent;
			if (n.getClass().equals(Fragment.class)) {
				Fragment f = (Fragment)n;
				fragments += _sdutil.printFragment(f);
				fragments += _sdutil.printInSequence(f, indent+"\t");
			} else if (n.getClass().equals(Operand.class)) {
				Operand o = (Operand) n;
				fragments += "Guard = " + o.getGuard() + "\n";
				for (Node n1: o.getNodes()) {
				    fragments += indent;
					if (n1.getClass().equals(Message.class)) {
					    fragments += _sdutil.printMessage((Message)n1);
					} else if (n1.getClass().equals(Fragment.class)) {
						Fragment f = (Fragment)n1;
						fragments += _sdutil.printFragment(f);
						fragments += _sdutil.printInSequence((Fragment)n1, indent+'\t');
					}
				}
			} else if (n.getClass().equals(Message.class)) {
			    fragments += _sdutil.printMessage((Message)n);
			}
		}
		
		return fragments;
	}
}
