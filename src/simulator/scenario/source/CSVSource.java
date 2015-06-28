package simulator.scenario.source;

import java.util.HashMap;

public class CSVSource implements Source {
	private HashMap<String, String> args;
	
	public CSVSource(String path) {
		args = new HashMap<String, String>();
		args.put("type", "body");
		args.put("path", path);
	}

	@Override
	public HashMap<String, String> getArgs() {
		return args;
	}
}
