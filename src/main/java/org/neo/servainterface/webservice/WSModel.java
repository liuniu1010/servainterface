package org.neo.servainterface.webservice;

import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.neo.servaaibase.util.CommonUtil;

public class WSModel {
    public static class AIChatParams {
        String userInput = "";
        String fileContent = "";

        public String getUserInput() {
            return userInput;
        }

        public void setUserInput(String inputUserInput) {
            userInput = inputUserInput == null?"":inputUserInput;
        }

        public String getFileContent() {
            return fileContent;
        }

        public void setFileContent(String inputFileContent) {
            fileContent = inputFileContent == null?"":inputFileContent;
        }
    }

    public static class AIGameFactoryParams {
        String job_id = "";
        String requirement = "";
        String code = "";

        public String getJob_id() {
            return job_id;
        }

        public void setJob_id(String input_job_id) {
            job_id = input_job_id == null?"":input_job_id;
        }

        public String getRequirement() {
            return requirement;
        }

        public void setRequirement(String input_requirement) {
            requirement = input_requirement == null?"":input_requirement;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String input_code) {
            code = input_code == null?"":input_code;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }

            if(obj == null) {
                return false;
            }

            if(!(obj instanceof WSModel.AIGameFactoryParams)) {
                return false;
            }

            WSModel.AIGameFactoryParams params = (WSModel.AIGameFactoryParams)obj;

            return (this.getJob_id().equals(params.getJob_id())) && 
                   (this.getRequirement().equals(params.getRequirement())) &&
                   (this.getCode().equals(params.getCode()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(job_id, requirement, code);
        }

        public String toJson() {
            Gson gson = new Gson();
            JsonObject jsonBody = new JsonObject();

            jsonBody.addProperty("job_id", getJob_id());
            jsonBody.addProperty("requirement", getRequirement());
            jsonBody.addProperty("code", getCode());

            return CommonUtil.alignJson(gson.toJson(jsonBody));
        }

        public static WSModel.AIGameFactoryParams fromJson(String json) {
            WSModel.AIGameFactoryParams instance = new WSModel.AIGameFactoryParams();

            JsonElement element = JsonParser.parseString(json);
            JsonObject jsonObject = element.getAsJsonObject();
            if(jsonObject.has("job_id")) {
                instance.setJob_id(jsonObject.get("job_id").getAsString());
            }
            if(jsonObject.has("requirement")) {
                instance.setRequirement(jsonObject.get("requirement").getAsString());
            }
            if(jsonObject.has("code")) {
                instance.setCode(jsonObject.get("code").getAsString());
            }

            return instance;
        }
    }

    public static class AIChatResponse {
        private boolean isSuccess = true;
        private String message = "";

        public AIChatResponse(boolean inputIsSuccess, String inputMessage) {
            isSuccess = inputIsSuccess;
            message = inputMessage == null?"":inputMessage;
        }

        public boolean getIsSuccess() {
            return isSuccess;
        }

        public void setIsSuccess(boolean inputIsSuccess) {
            isSuccess = inputIsSuccess;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String inputMessage) {
            message = inputMessage == null?"":inputMessage;
        }
    }

    public static class AIGameFactoryResponse {
        public final static String JOB_STATUS_INPROGRESS = "inprogress";
        public final static String JOB_STATUS_DONE = "done";
        public final static String JOB_STATUS_FAILED = "failed";
        public final static String JOB_STATUS_CANCELLED = "cancelled";

        private String job_id = "";
        private String job_status = "";   // inprogress/done/failed 
        private String code = "";
        private String message = "";

        public AIGameFactoryResponse(String input_job_id) {
            job_id = input_job_id == null?"":input_job_id;
        }

        public String getJob_id() {
            return job_id;
        }

        public void setJob_id(String input_job_id) {
            job_id = input_job_id == null?"":input_job_id;
        }

        public String getJob_status() {
            return job_status;
        }

        public void setJob_status(String input_job_status) {
            job_status = input_job_status == null?"":input_job_status;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String input_code) {
            code = input_code == null?"":input_code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String inputMessage) {
            message = inputMessage == null?"":inputMessage;
        }
    }
}
