package util;


import com.google.cloud.Timestamp;

public class ValidityData {
    public Timestamp from;
    public Timestamp to;
    public String verificador;

    public ValidityData(){
    }

    public ValidityData(Timestamp from, Timestamp to, String verificador){
        this.from = from;
        this.to = to;
        this.verificador = verificador;
    }
}
