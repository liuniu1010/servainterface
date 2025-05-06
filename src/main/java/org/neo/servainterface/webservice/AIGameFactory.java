package org.neo.servainterface.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.neo.servaaibase.NeoAIException;
import org.neo.servaaibase.model.AIModel;

import org.neo.servaaiagent.ifc.UtilityAgentIFC;
import org.neo.servaaiagent.impl.UtilityAgentInMemoryImpl;

@Path("/aigamefactory")
public class AIGameFactory {
    final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AIGameFactory.class);

    @POST
    @Path("/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(WSModel.AIChatParams params) {
        try {
            WSModel.AIChatResponse chatResponse = innerGenerate(params);
            if(chatResponse.getIsSuccess()) {
                return generateHttpResponse(Response.Status.OK, chatResponse);
            }
            else {
                return generateHttpResponse(Response.Status.INTERNAL_SERVER_ERROR, chatResponse);
            }
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIChatResponse chatResponse = new WSModel.AIChatResponse(false, ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), chatResponse);
        }
    }

    @POST
    @Path("/createjob")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(WSModel.AIGameFactoryParams params) {
        try {
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerCreateJob(params);
            return generateHttpResponse(Response.Status.ACCEPTED, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(params.getJob_id());
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), gameFactoryResponse);
        }
    }

    @POST
    @Path("/checkjob")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkJob(WSModel.AIGameFactoryParams params) {
        try {
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerCheckJob(params);
            return generateHttpResponse(Response.Status.OK, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(params.getJob_id());
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), gameFactoryResponse);
        }
    }

    @POST
    @Path("/canceljob")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelJob(WSModel.AIGameFactoryParams params) {
        try {
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerCancelJob(params);
            return generateHttpResponse(Response.Status.OK, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(params.getJob_id());
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), gameFactoryResponse);
        }
    }

    private WSModel.AIChatResponse innerGenerate(WSModel.AIChatParams params) throws Exception {
        String userInput = params.getUserInput();
        String fileContent = params.getFileContent();

        UtilityAgentIFC utilityAgent = UtilityAgentInMemoryImpl.getInstance();
        AIModel.ChatResponse chatResponse = utilityAgent.generatePageCode(userInput, fileContent);

        WSModel.AIChatResponse response = new WSModel.AIChatResponse(chatResponse.getIsSuccess(), chatResponse.getMessage());
        return response;
    }

    private WSModel.AIGameFactoryResponse innerCreateJob(WSModel.AIGameFactoryParams params) {
        WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse("job_1");
        gameFactoryResponse.setJob_status(WSModel.AIGameFactoryResponse.JOB_STATUS_INPROGRESS);
        return gameFactoryResponse;
    }

    private WSModel.AIGameFactoryResponse innerCheckJob(WSModel.AIGameFactoryParams params) {
        WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(params.getJob_id());
        gameFactoryResponse.setJob_status(WSModel.AIGameFactoryResponse.JOB_STATUS_DONE);
        return gameFactoryResponse;
    }

    private WSModel.AIGameFactoryResponse innerCancelJob(WSModel.AIGameFactoryParams params) {
        WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(params.getJob_id());
        gameFactoryResponse.setJob_status(WSModel.AIGameFactoryResponse.JOB_STATUS_CANCELLED);
        return gameFactoryResponse;
    }

    private Response.Status decideHttpResponseStatus(Exception ex) {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    private Response generateHttpResponse(Response.Status httpStatus, Object entity) {
        return Response.status(httpStatus)
                       .entity(entity)
                       .build();
    }
}
