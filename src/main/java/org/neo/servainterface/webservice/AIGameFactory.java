package org.neo.servainterface.webservice;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletRequest;

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
import org.neo.servaaiagent.ifc.AccessAgentIFC;
import org.neo.servaaiagent.impl.UtilityAgentInMemoryImpl;
import org.neo.servaaiagent.impl.AccessAgentImpl;

@Path("v1/aigamefactory")
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
    public Response generate(@Context HttpServletRequest request, WSModel.AIGameFactoryParams params) {
        try {
            String sourceIP = getSourceIP(request);
            checkAccessibilityOnAction(sourceIP);
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerGenerate(params);
            return generateHttpResponse(Response.Status.OK, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse("");
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), gameFactoryResponse);
        }
    }

    @POST
    @Path("/jobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(@Context HttpServletRequest request, WSModel.AIGameFactoryParams params) {
        try {
            String sourceIP = getSourceIP(request);
            checkAccessibilityOnAction(sourceIP);
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerCreateJob(params);
            return generateHttpResponse(Response.Status.ACCEPTED, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse("");
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), gameFactoryResponse);
        }
    }

    @GET
    @Path("/jobs/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {
        try {
            String sourceIP = getSourceIP(request);
            checkAccessibilityOnAction(sourceIP);
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerCheckJob(jobId);
            return generateHttpResponse(Response.Status.OK, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(jobId);
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), gameFactoryResponse);
        }
    }

    private WSModel.AIGameFactoryResponse innerGenerate(WSModel.AIGameFactoryParams params) throws Exception {
        String prompt = params.getPrompt();
        String code = params.getCode();

        UtilityAgentIFC utilityAgent = UtilityAgentInMemoryImpl.getInstance();
        AIModel.ChatResponse chatResponse = utilityAgent.generatePageCode(prompt, code);

        WSModel.AIGameFactoryResponse response = new WSModel.AIGameFactoryResponse("");
        if(chatResponse.getIsSuccess()) {
            response.setJobStatus(WSModel.AIGameFactoryResponse.JOB_STATUS_DONE);
            response.setCode(chatResponse.getMessage());
        }
        else {
            response.setJobStatus(WSModel.AIGameFactoryResponse.JOB_STATUS_FAILED);
            response.setMessage(chatResponse.getMessage());
        }
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
        gameFactoryResponse.setJobStatus(job.getJobStatus());
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

        String prompt = gameFactoryParams.getPrompt();
        String code = gameFactoryParams.getCode();

        UtilityAgentIFC utilityAgent = UtilityAgentInMemoryImpl.getInstance();
        AIModel.ChatResponse chatResponse = utilityAgent.generatePageCode(prompt, code);
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

    private WSModel.AIGameFactoryResponse innerCheckJob(String jobId) throws Exception {
        AIModel.NeoJob neoJob = checkNeoJobInDB(jobId);

        WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(neoJob.getJobId());
        gameFactoryResponse.setJobStatus(neoJob.getJobStatus());
        gameFactoryResponse.setCode(neoJob.getJobOutcome());

        return gameFactoryResponse;
    }

    private AIModel.NeoJob checkNeoJobInDB(String jobId) throws Exception {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        return (AIModel.NeoJob)dbService.executeQueryTask(new AIGameFactory() {
            @Override
            public Object query(DBConnectionIFC dbConnection) {
                try {
                    return checkNeoJobInDB(dbConnection, jobId);
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

    private AIModel.NeoJob checkNeoJobInDB(DBConnectionIFC dbConnection, String jobId) throws Exception {
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
        sqlParams.add(jobId);
        sqlParams.add(new Date());
        SQLStruct sqlStruct = new SQLStruct(sql, sqlParams);

        VersionEntity versionEntity = dbConnection.querySingleAsVersionEntity(AIModel.NeoJob.ENTITYNAME, sqlStruct);
        if(versionEntity == null) {
            throw new NeoAIException(NeoAIException.NEOAIEXCEPTION_JOB_NOTFOUND);
        }

        AIModel.NeoJob neoJob = new AIModel.NeoJob(versionEntity);
        return neoJob;
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
        CacheControl cc = new CacheControl();
        cc.setNoStore(true);

        return Response.status(httpStatus)
                       .entity(entity)
                       .cacheControl(cc)
                       .build();
    }

    private String getSourceIP(HttpServletRequest request) {
        String sourceIP = request.getHeader("X-Forwarded-For");
        if (sourceIP == null || sourceIP.isEmpty()) {
            sourceIP = request.getRemoteAddr(); // fallback
        }
        else {
            // In case there are multiple IPs (comma-separated), take the first one
            sourceIP = sourceIP.split(",")[0].trim();
        }
        return sourceIP;
    }

    private void checkAccessibilityOnAction(String sourceIP) {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        dbService.executeQueryTask(new DBQueryTaskIFC() {
            @Override
            public Object query(DBConnectionIFC dbConnection) {
                try {
                    innerCheckAccessibilityOnAction(dbConnection, sourceIP);
                }
                catch(NeoAIException nex) {
                    throw nex;
                }
                catch(Exception ex) {
                    throw new NeoAIException(ex.getMessage(), ex);
                }
                return null;
            }
        }); 
    }

    private void innerCheckAccessibilityOnAction(DBConnectionIFC dbConnection, String sourceIP) {
        AccessAgentIFC accessAgent = AccessAgentImpl.getInstance();
        if(accessAgent.verifyIP(dbConnection, sourceIP)) {
            return;
        }
        // by default, deny access
        throw new NeoAIException("access denied from servainterface!");
    }
}
