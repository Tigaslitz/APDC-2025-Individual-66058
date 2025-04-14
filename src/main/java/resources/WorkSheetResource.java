package resources;

import com.google.cloud.Role;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import util.AuthToken;
import util.WorksheetData;

import Enum.Adjudicacao;
import Enum.WorkState;
import Enum.Roles;
import java.util.logging.Logger;

@Path("/worksheet")
public class WorkSheetResource {

    private static final Logger LOG = Logger.getLogger(WorkSheetResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory worksheetKeyFactory = datastore.newKeyFactory().setKind("WorkSheet");
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");


    private final Gson g = new Gson();

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createWorkSheet(WorksheetData data, @Context HttpServletRequest request) {

        AuthToken token = (AuthToken) request.getAttribute("authToken");

        if (!data.validRegistration() || !isValidPartner(data.contaEntidade)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }

        String username = token.username;
        String role = token.role;

        if (!Roles.valueOf(role).equals(Roles.BACKOFFICE)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Role not authorized").build();
        }


        Key workSheetKey = worksheetKeyFactory.newKey(data.referencia);
        Transaction txn = datastore.newTransaction();

        try {
            Adjudicacao adjudicacao = Adjudicacao.valueOf(data.estadoAdjudicacao.toUpperCase());
            Entity.Builder builder = Entity.newBuilder(workSheetKey)
                    .set("obra_referencia", data.referencia)
                    .set("obra_descricao", data.descricao)
                    .set("obra_tipo", data.tipoAlvo.toUpperCase())
                    .set("obra_estado_adjudicacao", data.estadoAdjudicacao.toUpperCase());

            if (adjudicacao.equals(Adjudicacao.ADJUDICADO)) {
                builder.set("data_adjudicacao", data.dataAdjudicacao)
                        .set("data_inicio_obra", data.inicioObra)
                        .set("data_fim_obra", data.fimObra)
                        .set("conta_entidade", data.contaEntidade)
                        .set("empresa_adjudicada", data.nomeEmpresa)
                        .set("nif_empresa", data.nifEmpresa)
                        .set("estado_obra", data.estadoObra.toUpperCase())
                        .set("observacoes", data.observacoes);
            }

            // BACKOFFICE pode criar/atualizar tudo
            txn.put(builder.build());
            txn.commit();
            return Response.ok("Worksheet saved successfully").build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Erro ao registar folha de obra: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erro interno").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }


    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkSheet(WorksheetData data, @Context HttpServletRequest request) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");

        String username = token.username;
        Roles role = Roles.valueOf(token.role);
        if (!data.validUpdate() || !isValidPartner(data.contaEntidade))
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();

        Key workSheetKey = worksheetKeyFactory.newKey(data.referencia);
        Transaction txn = datastore.newTransaction();

        try {
            if (!role.equals(Roles.BACKOFFICE) && !role.equals(Roles.PARTNER)) {
                return Response.status(Response.Status.FORBIDDEN).entity("Role not authorized").build();
            }
            Entity worksheet = txn.get(workSheetKey);
            // PARTNER só pode alterar o estado da obra se for o responsável
            if (role.equals(Roles.PARTNER)) {
                if (worksheet == null || !worksheet.getString("conta_entidade").equals(username)) {
                    txn.rollback();
                    return Response.status(Response.Status.FORBIDDEN).entity("You cannot update this worksheet").build();
                }
            }
            Entity updated = Entity.newBuilder(worksheet)
                    .set("estado_obra", data.estadoObra.toUpperCase())
                    .build();


            txn.put(updated);
            txn.commit();
            return Response.ok("Worksheet status updated by " + token.role).build();
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Erro ao registar folha de obra: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erro interno").build();
    } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }


    private boolean isValidPartner(String contaEntidade) {
        if (contaEntidade != null) {
            Key userKey = userKeyFactory.newKey(contaEntidade);
            Entity user = datastore.get(userKey);

            if (user == null) {
                return false;
            }
            String role = user.getString("user_role");
            return Roles.valueOf(role).equals(Roles.PARTNER);
        }
        return true;
    }
}

