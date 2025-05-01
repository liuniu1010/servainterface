package org.neo.servainterface.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/aigamefactory")
public class AIGameFactory {
    final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AIGameFactory.class);

    @POST
    @Path("/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WSModel.AIChatResponse generate(WSModel.AIChatParams params) {
        try {
            logger.info("Enter into generate()");
            return innerGenerate(params);
        }
        catch(Exception ex) {
            return new WSModel.AIChatResponse(false, ex.getMessage());
        }
    }

    private WSModel.AIChatResponse innerGenerate(WSModel.AIChatParams params) throws Exception {
        String userInput = params.getUserInput();
        String fileContent = params.getFileContent();

        String toReturn = "userInput: " + userInput;
        toReturn += "\nfileContent: " + fileContent;
        WSModel.AIChatResponse response = new WSModel.AIChatResponse(true, toReturn);
        return response;
    }
}
