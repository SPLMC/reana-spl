package parsing.SplGeneratorModels;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import parsing.SplGeneratorModels.Fragment;
import parsing.SplGeneratorModels.Lifeline;
import parsing.SplGeneratorModels.Message;
import parsing.SplGeneratorModels.SequenceDiagram;
import parsing.SplGeneratorModels.SequenceDiagramElement;
import parsing.SplGeneratorModels.SPLFilePersistence;
import tool.RDGNode;
import fdtmc.*;

public class SequenceDiagramTransformer {

	RDGNode root;
	HashMap<String, fdtmc.State> fdtmcStateById;

	public SequenceDiagramTransformer() {
		fdtmcStateById = new HashMap<String, fdtmc.State>();
		root = null;
	}

	public RDGNode transformSD(SequenceDiagram s, String id) {
		FDTMC f = new FDTMC();
		f.setVariableName(s.getName() + "_s");
		RDGNode answer = new RDGNode(id, s.getGuardCondition(), f);
		root = answer;

		State s0 = f.createInitialState();
		f.createErrorState();
		State target = transformSdElement(s.getElements(), f);
		f.createTransition(s0, target, "", Double.toString(1.0));

		return answer;
	}

	private State transformSdElement(LinkedList<SequenceDiagramElement> sde,
			FDTMC f) {
		State source;
		State target;

		// SequenceDiagramElement e = sde.removeFirst();
		SequenceDiagramElement e = null;
		String sdClass;
		if (sde.isEmpty()) {
			target = f.createSuccessState();
			return target;
		} else {
			//e = sde.removeFirst();
			e = sde.getFirst();
			LinkedList<SequenceDiagramElement> newList = new LinkedList<SequenceDiagramElement>();
			for (int i = 1; i < sde.size(); i++) {
				newList.push(sde.get(i));
			}
			sdClass = e.getClass().getSimpleName();
			target = transformSdElement(newList, f);
		}

		source = f.createState();
		
		if (e.getName().contentEquals("n16")) {
			System.out.println("Temperature");
		}
		
		if (e.getName().contentEquals("n13")) {
			System.out.println("PulseRate");
		}
		
	/*	if (e.getName().contentEquals("n6")) {
			System.out.println("Mais um memory");
		}
*/
		switch (sdClass) {
		case "Message":
			if (this.root.getId().contentEquals("n5")) {
				System.out.println("Passou message n5");
			}
			Message m = (Message) e;
			double reliability = new BigDecimal(m.getProbability()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
			double complement = new BigDecimal(1-m.getProbability()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
			if (m.getType() == Message.SYNCHRONOUS || m.getType() == Message.ASYNCHRONOUS) {
				f.createTransition(source, target, m.getName(),
						Double.toString(reliability));
				// PARAM doesn't like 0-valued transitions... it SEGFAULTs without mercy!
				if (Double.compare(reliability, 1.0) != 0) {
					f.createTransition(source, f.getErrorState(), m.getName(),
							Double.toString(complement));
				}
			}
			break;

		case "Fragment":
			Fragment fr = (Fragment) e;
			if (fr.getType() == Fragment.OPTIONAL) {
				SequenceDiagram onlySD = fr.getSequenceDiagrams()
						.getFirst();
				SequenceDiagramTransformer transformer = new SequenceDiagramTransformer();
				if (onlySD.getGuardCondition().contentEquals("Memory")) {
					System.out.println("Mais um memory");
				}
				//RDGNode dependencyNode = transformer.transformSD(onlySD, RDGNode.getNextId());
				RDGNode dependencyNode = transformer.transformSD(onlySD, onlySD.getName());
				this.root.addDependency(dependencyNode);

				String dependencyName = dependencyNode.getId();
                f.createInterface(dependencyName, source, target, f.getErrorState());
			} else if (fr.getType() == Fragment.ALTERNATIVE) {
				for (SequenceDiagram s : fr.getSequenceDiagrams()) {
					target = transformSdElement(s.getElements(), f);
					double probability = new BigDecimal(1 / fr.getSequenceDiagrams().size()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
					f.createTransition(source, target, "alt", Double
							.toString(probability));
				}
			}
			break;

		default:
			break;
		}

		return source;
	}

}
