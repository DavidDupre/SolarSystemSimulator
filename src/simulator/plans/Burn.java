package simulator.plans;

public class Burn {
	private Command command;
	public double epoch;
	public boolean isFinished = false;
	
	public Burn(double epoch, Command command) {
		this.command = command;
		this.epoch = epoch;
	}
	
	public void execute() {
		command.run();
		isFinished = true;
	}
	
	public interface Command {
		public void run();
	}
}
