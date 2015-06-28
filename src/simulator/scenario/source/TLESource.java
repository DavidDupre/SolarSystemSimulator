package simulator.scenario.source;

import java.util.HashMap;

public class TLESource implements Source {
	private HashMap<String, String> args;
	
	public TLESource(String category) {
		args = new HashMap<String, String>();
		args.put("type", "ship");
		args.put("category", category);
	}
	
	public TLESource(String category, String name) {
		args = new HashMap<String, String>();
		args.put("type", "ship");
		args.put("category", category);
		args.put("name", name);
	}

	@Override
	public HashMap<String, String> getArgs() {
		return args;
	}
}
