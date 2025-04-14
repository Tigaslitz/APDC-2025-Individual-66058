package util;

import Enum.Adjudicacao;
import Enum.WorkState;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class WorksheetData {
    public String referencia;
    public String descricao;
    public String tipoAlvo;
    public String estadoAdjudicacao;
    public String dataAdjudicacao;
    public String inicioObra;
    public String contaEntidade;
    public String fimObra;
    public String nomeEmpresa;
    public String nifEmpresa;
    public String estadoObra;
    public String observacoes;



    public WorksheetData(){
    }

    public WorksheetData (String referencia, String descricao, String tipoAlvo, String estadoAdjudicacao){
        this.referencia = referencia;
        this.descricao = descricao;
        this.tipoAlvo = tipoAlvo;
        this.estadoAdjudicacao = estadoAdjudicacao;
    }

    public boolean validRegistration() {
        return nonEmptyOrBlankField(referencia) &&
                nonEmptyOrBlankField(descricao) &&
                nonEmptyOrBlankField(tipoAlvo) &&
                validAdjudication(estadoAdjudicacao) &&
                validWorkState(estadoObra);
    }


    private boolean validWorkState(String estadoObra) {
        try{
            WorkState.valueOf(estadoObra.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private boolean validAdjudication(String estadoAdjudicacao) {
        if (estadoAdjudicacao != null){
            if (Adjudicacao.valueOf(estadoAdjudicacao).equals(Adjudicacao.ADJUDICADO))
                return nonEmptyOrBlankField(dataAdjudicacao) &&
                        nonEmptyOrBlankField(inicioObra) &&
                        nonEmptyOrBlankField(fimObra) &&
                        nonEmptyOrBlankField(contaEntidade) &&
                        nonEmptyOrBlankField(nomeEmpresa) &&
                        nonEmptyOrBlankField(nifEmpresa) &&
                        nonEmptyOrBlankField(estadoObra) &&
                        nonEmptyOrBlankField(observacoes);
            else if(Adjudicacao.valueOf(estadoAdjudicacao).equals(Adjudicacao.NAO_ADJUDICADO))
                return dataAdjudicacao == null && inicioObra == null && fimObra == null;
        }
        return false;
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    public boolean validUpdate() {
        return nonEmptyOrBlankField(contaEntidade) && nonEmptyOrBlankField(estadoObra) && nonEmptyOrBlankField(referencia);
    }
}
