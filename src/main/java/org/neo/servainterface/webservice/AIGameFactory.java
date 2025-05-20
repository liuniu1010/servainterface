package org.neo.servainterface.webservice;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;
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
    final static String RAPIDAPI_SECRET = "X-RapidAPI-Proxy-Secret";
    final static String JOB_TYPE = "gamefactory";

    @Override
    public Object save(DBConnectionIFC dbConnection) {
        return null;
    }

    @Override
    public Object query(DBConnectionIFC dbConnection) {
        return null;
    }

    private static final ExecutorService JOB_POOL = Executors.newFixedThreadPool(CommonUtil.getConfigValueAsInt("interfaceThreadPoolSize"));

    static {                                    // graceful shutdown
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> JOB_POOL.shutdown()));
    }

    @OPTIONS
    @Path("/generate")
    public Response handleGeneratePreflight(@Context HttpHeaders headers) {
        return handlePreflight(headers);
    }

    @POST
    @Path("/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(@Context HttpServletRequest request, @Context HttpHeaders headers, WSModel.AIGameFactoryParams params) {
        try {
            checkAccessibilityOnAction(request, headers);
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerGenerate(params);
            return generateHttpResponse(Response.Status.OK, headers, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse("");
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), headers, gameFactoryResponse);
        }
    }

    @OPTIONS
    @Path("/jobs")
    public Response handleCreateJobPreflight(@Context HttpHeaders headers) {
        return handlePreflight(headers);
    }

    @POST
    @Path("/jobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(@Context HttpServletRequest request, @Context HttpHeaders headers, WSModel.AIGameFactoryParams params) {
        try {
            checkAccessibilityOnAction(request, headers);
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerCreateJob(params);
            return generateHttpResponse(Response.Status.ACCEPTED, headers, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse("");
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), headers, gameFactoryResponse);
        }
    }

    @OPTIONS
    @Path("/jobs/{jobId}")
    public Response handleCheckJobPreflight(@Context HttpHeaders headers) {
        return handlePreflight(headers);
    }

    @GET
    @Path("/jobs/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkJob(@Context HttpServletRequest request, @Context HttpHeaders headers, @PathParam("jobId") String jobId) {
        try {
            checkAccessibilityOnAction(request, headers);
            WSModel.AIGameFactoryResponse gameFactoryResponse = innerCheckJob(jobId);
            return generateHttpResponse(Response.Status.OK, headers, gameFactoryResponse);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            WSModel.AIGameFactoryResponse gameFactoryResponse = new WSModel.AIGameFactoryResponse(jobId);
            gameFactoryResponse.setMessage(ex.getMessage());
            return generateHttpResponse(decideHttpResponseStatus(ex), headers, gameFactoryResponse);
        }
    }

    private WSModel.AIGameFactoryResponse innerGenerate(WSModel.AIGameFactoryParams params) throws Exception {
        AIModel.NeoJob job = createNeoJobInDB(params);
        executeJob(job.getJobId());
        WSModel.AIGameFactoryResponse gameFactoryResponse = innerCheckJob(job.getJobId());
        return gameFactoryResponse;
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

    private WSModel.AIGameFactoryParams extractGameFactoryParamsFromJobId(String jobId) throws Exception {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        return (WSModel.AIGameFactoryParams)dbService.executeQueryTask(new AIGameFactory() {
            @Override
            public Object query(DBConnectionIFC dbConnection) {
                try {
                    return extractGameFactoryParamsFromJobId(dbConnection, jobId);
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
        sql += " and jobtype = ?";

        List<Object> sqlParams = new ArrayList<Object>();
        sqlParams.add(jobId);
        sqlParams.add(new Date());
        sqlParams.add(JOB_TYPE);

        SQLStruct sqlStruct = new SQLStruct(sql, sqlParams);

        String jobparamsInJson = (String)dbConnection.queryScalar(sqlStruct);
        return WSModel.AIGameFactoryParams.fromJson(jobparamsInJson);
    }

    private void executeJob(String jobId) {
        try {
            innerExecuteJob(jobId);
        }
        catch(Exception ex) {
            AIModel.NeoJob job = new AIModel.NeoJob(jobId);
            job.setJobStatus(WSModel.AIGameFactoryResponse.JOB_STATUS_FAILED);
            job.setJobOutcome("");
            job.setMessage(ex.getMessage());

            fillbackNeoJob(job);
        }
    }

    private void fillbackNeoJob(AIModel.NeoJob job) {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        dbService.executeSaveTask(new AIGameFactory() {
            @Override
            public Object save(DBConnectionIFC dbConnection) {
                try {
                    fillbackNeoJob(dbConnection, job);
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

    private void innerExecuteJob(String jobId) throws Exception {
        WSModel.AIGameFactoryParams gameFactoryParams = extractGameFactoryParamsFromJobId(jobId); 

        String prompt = gameFactoryParams.getPrompt();
        String code = gameFactoryParams.getCode();

        UtilityAgentIFC utilityAgent = UtilityAgentInMemoryImpl.getInstance();
        AIModel.ChatResponse chatResponse = utilityAgent.generatePageCode(prompt, code);
        if(chatResponse.getIsSuccess()) {
            AIModel.NeoJob job = new AIModel.NeoJob(jobId);
            job.setJobStatus(WSModel.AIGameFactoryResponse.JOB_STATUS_DONE);
            job.setJobOutcome(chatResponse.getMessage());
            job.setMessage("");

            fillbackNeoJob(job);
        }
        else {
            AIModel.NeoJob job = new AIModel.NeoJob(jobId);
            job.setJobStatus(WSModel.AIGameFactoryResponse.JOB_STATUS_FAILED);
            job.setJobOutcome("");
            job.setMessage(chatResponse.getMessage());

            fillbackNeoJob(job);
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
        sql += " and jobtype = ?";

        List<Object> sqlParams = new ArrayList<Object>();
        sqlParams.add(jobId);
        sqlParams.add(new Date());
        sqlParams.add(JOB_TYPE);
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

    private void checkAccessibilityOnAction(HttpServletRequest request, HttpHeaders headers) {
        DBServiceIFC dbService = ServiceFactory.getDBService();
        dbService.executeQueryTask(new DBQueryTaskIFC() {
            @Override
            public Object query(DBConnectionIFC dbConnection) {
                try {
                    innerCheckAccessibilityOnAction(dbConnection, request, headers);
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

    private void innerCheckAccessibilityOnAction(DBConnectionIFC dbConnection, HttpServletRequest request, HttpHeaders headers) {
        String sourceIP = getSourceIP(request);
        String secretValue = request.getHeader(RAPIDAPI_SECRET);

        if(logger.isDebugEnabled()) {
            logger.debug("sourceIP = " + sourceIP);
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                logger.debug("Header: " + headerName + " = " + headerValue);
            }

            String requestedHeaders = headers.getRequestHeaders().getFirst("Access-Control-Request-Headers");
            logger.debug("Access-Control-Request-Headers = " + requestedHeaders);
        }

        AccessAgentIFC accessAgent = AccessAgentImpl.getInstance();
        if(accessAgent.verifyRegion(dbConnection, sourceIP)) {
            return;
        }

        if(accessAgent.verifySecret(dbConnection, RAPIDAPI_SECRET, secretValue)) {
            return;
        }
        // by default, deny access
        throw new NeoAIException("access denied!");
    }

    private Response handlePreflight(HttpHeaders headers) {
        String requestedHeaders = headers.getRequestHeaders().getFirst("Access-Control-Request-Headers");

        if (requestedHeaders == null || requestedHeaders.isEmpty()) {
            requestedHeaders = "Content-Type, Accept, X-RapidAPI-Key, X-RapidAPI-Host";
        }

        return Response.ok()
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                       .header("Access-Control-Allow-Headers", requestedHeaders)
                       .build();
    }

    private Response generateHttpResponse(Response.Status httpStatus, HttpHeaders headers, Object entity) {
        String requestedHeaders = headers.getRequestHeaders().getFirst("Access-Control-Request-Headers");
    
        if (requestedHeaders == null || requestedHeaders.isEmpty()) {
            requestedHeaders = "Content-Type, Accept, X-RapidAPI-Key, X-RapidAPI-Host";
        }

        return Response.status(httpStatus)
                       .entity(entity)
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                       .header("Access-Control-Allow-Headers", requestedHeaders)
                       .build();
    }
}
