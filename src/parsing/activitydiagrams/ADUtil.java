package parsing.activitydiagrams;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ADUtil {
    private static final Logger LOGGER = Logger.getLogger(ADUtil.class.getName());

	public static void printAll(ADReader adParser) { // jogar pra cima?? DiagramAPI
	    if (LOGGER.isLoggable(Level.FINER)) {
	        String message = "Activity Diagram " + (adParser.getIndex() + 1) + ": " + adParser.getName() + "\n\n";
	        message += printInSequence(adParser);
	        LOGGER.finer(message);
	    }
	}

	private static String printInSequence(ADReader adParser) {
	    String message = "Activities:\n";
		for (Activity a : adParser.getActivities()) {
			message += a.print();
			if (!a.getIncoming().isEmpty()) {
				message = printMessage(message, a, "\tIncoming Edges:\n");
			}
			if (!a.getOutgoing().isEmpty()) {
				message = printMessage(message, a, "\tOutgoing Edges:\n");
			}
		}
		return message;
	}
	
	private static String printMessage(String message, Activity a, String str){
		message += str;
		for (Edge e : a.getOutgoing()) {
		    message += "\t\t";
		    message += e.print();
		}
		
		return message;
	}
}
