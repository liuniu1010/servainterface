package org.neo.servainterface.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    public WSModel.AIChatResponse generate(@Context HttpServletRequest request, @Context HttpServletResponse response, WSModel.AIChatParams params) {
        try {
            return innerGenerate(params);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            standardHandleException(ex, response);
        }
        return null;
    }

    @POST
    @Path("/createjob")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WSModel.AIGameFactoryResponse createJob(@Context HttpServletRequest request, @Context HttpServletResponse response, WSModel.AIGameFactoryParams params) {
        try {
            return innerCreateJob(params);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            standardHandleException(ex, response);
        }
        return null;
    }

    @POST
    @Path("/retrievejob")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WSModel.AIGameFactoryResponse retrieveJob(@Context HttpServletRequest request, @Context HttpServletResponse response, WSModel.AIGameFactoryParams params) {
        try {
            return innerRetrieveJob(params);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            standardHandleException(ex, response);
        }
        return null;
    }

    @POST
    @Path("/canceljob")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WSModel.AIGameFactoryResponse cancelJob(@Context HttpServletRequest request, @Context HttpServletResponse response, WSModel.AIGameFactoryParams params) {
        try {
            return innerCancelJob(params);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            standardHandleException(ex, response);
        }
        return null;
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
        return null;
    }

    private WSModel.AIGameFactoryResponse innerRetrieveJob(WSModel.AIGameFactoryParams params) {
        return null;
    }

    private WSModel.AIGameFactoryResponse innerCancelJob(WSModel.AIGameFactoryParams params) {
        return null;
    }

    private void standardHandleException(Exception ex, HttpServletResponse response) {
        terminateConnection(decideHttpResponseStatus(ex), ex.getMessage(), response);
    }

    private int decideHttpResponseStatus(Exception ex) {
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private void terminateConnection(int httpStatus, String message, HttpServletResponse response) {
        try {
            response.setStatus(httpStatus);
            response.getWriter().write(message);
            response.flushBuffer();
            return;
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
