package org.neo.servainterface.webservice;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.neo.servaframe.interfaces.DBConnectionIFC;
import org.neo.servaframe.interfaces.DBServiceIFC;
import org.neo.servaframe.interfaces.DBQueryTaskIFC;
import org.neo.servaframe.interfaces.DBSaveTaskIFC;
import org.neo.servaframe.model.SQLStruct;
import org.neo.servaframe.model.VersionEntity;
import org.neo.servaframe.ServiceFactory;

import org.neo.servaaibase.NeoAIException;
import org.neo.servaaibase.model.AIModel;
import org.neo.servaaibase.util.CommonUtil;

import org.neo.servaaiagent.ifc.UtilityAgentIFC;
import org.neo.servaaiagent.impl.UtilityAgentInMemoryImpl;

@Path("/aigamefactory")
public class AIGameFactory implements DBQueryTaskIFC, DBSaveTaskIFC {
    final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AIGameFactory.class);

    @Override
    public Object save(DBConnectionIFC dbConnection) {
        return null;
    }

    @Override
    public Object query(DBConnectionIFC dbConnection) {
        return null;
    }

    private static final ExecutorService JOB_POOL =
        Executors.newFixedThreadPool(
            Integer.parseInt(System.getProperty("job.pool.size", "32")));

    static {                                    // graceful shutdown
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> JOB_POOL.shutdown()));
    }

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
        AIModel.NeoJob job = createNeoJobInDB(params);

        CompletableFuture.runAsync(() -> {
            try {
                executeJob(job.getJobId());
            } 
            catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }, JOB_POOL);

        WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(job.getJobId());
        gameFactoryResponse.setJob_status(job.getJobStatus());
        return gameFactoryResponse;
    }

    private void executeJob(String jobId) {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        dbService.executeSaveTask(new AIGameFactory() {
            @Override
            public Object save(DBConnectionIFC dbConnection) {
                try {
                    executeJob(dbConnection, jobId);
                    return null;
                }
                catch(NeoAIException nex) {
                    throw nex;
                }
                catch(Exception ex) {
                    throw new NeoAIException(ex.getMessage(), ex);
                }
            }
        });
    }

    private WSModel.AIGameFactoryParams extractGameFactoryParamsFromJobId(DBConnectionIFC dbConnection, String jobId) throws Exception {
        String sql = "select jobparams";
        sql += " from neojob";
        sql += " where jobid = ?";
        sql += " and expiretime > ?";

        List<Object> sqlParams = new ArrayList<Object>();
        sqlParams.add(jobId);
        sqlParams.add(new Date());

        SQLStruct sqlStruct = new SQLStruct(sql, sqlParams);

        String jobparamsInJson = (String)dbConnection.queryScalar(sqlStruct);
        return WSModel.AIGameFactoryParams.fromJson(jobparamsInJson);
    }

    private void executeJob(DBConnectionIFC dbConnection, String jobId) {
        try {
            innerExecuteJob(dbConnection, jobId);
        }
        catch(Exception ex) {
            AIModel.NeoJob job = new AIModel.NeoJob(jobId);
            job.setJobStatus(WSModel.AIGameFactoryResponse.JOB_STATUS_FAILED);
            job.setJobOutcome("");
            job.setMessage(ex.getMessage());

            fillbackNeoJob(dbConnection, job);
        }
    }

    private void fillbackNeoJob(DBConnectionIFC dbConnection, AIModel.NeoJob job) {
        try {
            String sql = "update neojob";
            sql += " set jobstatus = ?";
            sql += ", joboutcome = ?";
            sql += ", message = ?";
            sql += " where jobid = ?";

            List<Object> sqlParams = new ArrayList<Object>();
            sqlParams.add(job.getJobStatus());
            sqlParams.add(job.getJobOutcome());
            sqlParams.add(job.getMessage());
            sqlParams.add(job.getJobId());

            SQLStruct sqlStruct = new SQLStruct(sql, sqlParams);
            dbConnection.execute(sqlStruct);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void innerExecuteJob(DBConnectionIFC dbConnection, String jobId) throws Exception {
        WSModel.AIGameFactoryParams gameFactoryParams = extractGameFactoryParamsFromJobId(dbConnection, jobId); 

        String userInput = gameFactoryParams.getRequirement();
        String fileContent = gameFactoryParams.getCode();

        UtilityAgentIFC utilityAgent = UtilityAgentInMemoryImpl.getInstance();
        AIModel.ChatResponse chatResponse = utilityAgent.generatePageCode(userInput, fileContent);
        if(chatResponse.getIsSuccess()) {
            AIModel.NeoJob job = new AIModel.NeoJob(jobId);
            job.setJobStatus(WSModel.AIGameFactoryResponse.JOB_STATUS_DONE);
            job.setJobOutcome(chatResponse.getMessage());
            job.setMessage("");

            fillbackNeoJob(dbConnection, job);
        }
        else {
            throw new NeoAIException(chatResponse.getMessage());
        }
    }

    private AIModel.NeoJob createNeoJobInDB(WSModel.AIGameFactoryParams params) {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        return (AIModel.NeoJob)dbService.executeSaveTask(new AIGameFactory() {
            @Override
            public Object save(DBConnectionIFC dbConnection) {
                try {
                    return createNeoJobInDB(dbConnection, params);
                }
                catch(NeoAIException nex) {
                    throw nex;
                }
                catch(Exception ex) {
                    throw new NeoAIException(ex.getMessage(), ex);
                }
            }
        });
    }

    private AIModel.NeoJob createNeoJobInDB(DBConnectionIFC dbConnection, WSModel.AIGameFactoryParams params) throws Exception {
        int JOB_ID_LENGTH = 10;
        String JOB_TYPE = "gamefactory";
        String newJobId = CommonUtil.getRandomString(JOB_ID_LENGTH);
        params.setJob_id(newJobId);
        String paramsInJson = params.toJson();
        Date createtime = new Date();
        int expireMinutes = CommonUtil.getConfigValueAsInt(dbConnection, "jobExpireMinutes");
        Date expiretime = CommonUtil.addTimeSpan(createtime, Calendar.MINUTE, expireMinutes); 

        AIModel.NeoJob job = new AIModel.NeoJob(newJobId);
        job.setJobType(JOB_TYPE);
        job.setJobStatus(WSModel.AIGameFactoryResponse.JOB_STATUS_INPROGRESS);
        job.setJobParams(paramsInJson);
        job.setCreatetime(createtime);
        job.setExpiretime(expiretime);

        dbConnection.insert(job.getVersionEntity());
        return job;
    }

    private WSModel.AIGameFactoryResponse innerCheckJob(WSModel.AIGameFactoryParams params) throws Exception {
        AIModel.NeoJob neoJob = checkNeoJobInDB(params);

        WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(neoJob.getJobId());
        gameFactoryResponse.setJob_status(neoJob.getJobStatus());
        gameFactoryResponse.setCode(neoJob.getJobOutcome());

        return gameFactoryResponse;
    }

    private AIModel.NeoJob checkNeoJobInDB(WSModel.AIGameFactoryParams params) throws Exception {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        return (AIModel.NeoJob)dbService.executeQueryTask(new AIGameFactory() {
            @Override
            public Object query(DBConnectionIFC dbConnection) {
                try {
                    return checkNeoJobInDB(dbConnection, params);
                }
                catch(NeoAIException nex) {
                    throw nex;
                }
                catch(Exception ex) {
                    throw new NeoAIException(ex.getMessage(), ex);
                }
            }
        });
    }

    private AIModel.NeoJob checkNeoJobInDB(DBConnectionIFC dbConnection, WSModel.AIGameFactoryParams params) throws Exception {
        String sql = "select id";
        sql += ", version";
        sql += ", jobid";
        sql += ", jobtype";
        sql += ", jobstatus";
        sql += ", joboutcome";
        sql += " from neojob";
        sql += " where jobid = ?";
        sql += " and expiretime > ?";

        List<Object> sqlParams = new ArrayList<Object>();
        sqlParams.add(params.getJob_id());
        sqlParams.add(new Date());
        SQLStruct sqlStruct = new SQLStruct(sql, sqlParams);

        VersionEntity versionEntity = dbConnection.querySingleAsVersionEntity(AIModel.NeoJob.ENTITYNAME, sqlStruct);
        if(versionEntity == null) {
            throw new NeoAIException(NeoAIException.NEOAIEXCEPTION_JOB_NOTFOUND);
        }

        AIModel.NeoJob neoJob = new AIModel.NeoJob(versionEntity);
        return neoJob;
    }

    private WSModel.AIGameFactoryResponse innerCancelJob(WSModel.AIGameFactoryParams params) {
        AIModel.NeoJob job = cancelNeoJobInDB(params);

        WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(job.getJobId());
        gameFactoryResponse.setJob_status(job.getJobStatus());
        return gameFactoryResponse;
    }

    private AIModel.NeoJob cancelNeoJobInDB(WSModel.AIGameFactoryParams params) {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        return (AIModel.NeoJob)dbService.executeSaveTask(new AIGameFactory() {
            @Override
            public Object save(DBConnectionIFC dbConnection) {
                try {
                    return cancelNeoJobInDB(dbConnection, params);
                }
                catch(NeoAIException nex) {
                    throw nex;
                }
                catch(Exception ex) {
                    throw new NeoAIException(ex.getMessage(), ex);
                }
            }
        });
    }

    private AIModel.NeoJob cancelNeoJobInDB(DBConnectionIFC dbConnection, WSModel.AIGameFactoryParams params) throws Exception {
        String sql = "update neojob";
        sql += " set jobstatus = ?";
        sql += " where jobid = ?";

        List<Object> sqlParams = new ArrayList<Object>();
        sqlParams.add(WSModel.AIGameFactoryResponse.JOB_STATUS_CANCELLED);
        sqlParams.add(params.getJob_id());

        SQLStruct sqlStruct = new SQLStruct(sql, sqlParams);

        dbConnection.execute(sqlStruct);

        return checkNeoJobInDB(dbConnection, params);
    }

    private Response.Status decideHttpResponseStatus(Exception ex) {
        if(ex instanceof NeoAIException) {
            NeoAIException nex = (NeoAIException)ex;
            if(nex.getCode() == NeoAIException.NEOAIEXCEPTION_JOB_NOTFOUND) {
                return Response.Status.NOT_FOUND;
            }
        }
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    private Response generateHttpResponse(Response.Status httpStatus, Object entity) {
        return Response.status(httpStatus)
                       .entity(entity)
                       .build();
    }
}
