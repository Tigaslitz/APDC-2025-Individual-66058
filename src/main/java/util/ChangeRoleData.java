package util;

public class ChangeRoleData {
    public String target;
    public String newRole;

    public ChangeRoleData(){
    }

    public ChangeRoleData(String target, String newRole){
        this.target = target;
        this.newRole = newRole;
    }
}
