package util;

public class ChangeAccStatusData {
    public String target;
    public String newState;

    public ChangeAccStatusData(){
    }

    public ChangeAccStatusData(String target, String newState){
        this.target = target;
        this.newState = newState;
    }
}
