package project.smartMessage;

public class AntState extends State{
	
	private boolean canReproduce;
	private boolean isExplorationAnt;
	private boolean isIdle;
	
	AntState(){
		super();
		canReproduce = false;
		isExplorationAnt = true;
	}
	
	public boolean isIdle() {
		return this.isIdle;
	}
	
	public void setIdle(boolean flag, AntAgent ant) {
		this.isIdle = flag;
	}
	
	public boolean isExplorationAnt() {
		return this.isExplorationAnt;
	}
	
	void changeToIntentionAnt() {
		this.isExplorationAnt = false;
	}
	
	public boolean canReproduce() {
		return this.canReproduce;
	}
	
	public void allowReproduction() {
		this.canReproduce = true;
	}
	
	void prohibitReproduction() {
		this.canReproduce = false;
	}
}
