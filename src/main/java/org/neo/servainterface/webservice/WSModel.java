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
    public static class AIGameFactoryParams {
        String prompt = "";
        String code = "";

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String inputPrompt) {
            prompt = inputPrompt == null?"":inputPrompt;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String inputCode) {
            code = inputCode == null?"":inputCode;
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

            return (this.getPrompt().equals(params.getPrompt())) &&
                   (this.getCode().equals(params.getCode()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(prompt, code);
        }

        public String toJson() {
            Gson gson = new Gson();
            JsonObject jsonBody = new JsonObject();

            jsonBody.addProperty("prompt", getPrompt());
            jsonBody.addProperty("code", getCode());

            return CommonUtil.alignJson(gson.toJson(jsonBody));
        }

        public static WSModel.AIGameFactoryParams fromJson(String json) {
            WSModel.AIGameFactoryParams instance = new WSModel.AIGameFactoryParams();

            JsonElement element = JsonParser.parseString(json);
            JsonObject jsonObject = element.getAsJsonObject();
            if(jsonObject.has("prompt")) {
                instance.setPrompt(jsonObject.get("prompt").getAsString());
            }
            if(jsonObject.has("code")) {
                instance.setCode(jsonObject.get("code").getAsString());
            }

            return instance;
        }
    }

    public static class AIGameFactoryResponse {
        public final static String JOB_STATUS_INPROGRESS = "inprogress";
        public final static String JOB_STATUS_DONE = "done";
        public final static String JOB_STATUS_FAILED = "failed";

        private String jobId = "";
        private String jobStatus = "";   // inprogress/done/failed 
        private String code = "";
        private String message = "";

        public AIGameFactoryResponse(String inputJobId) {
            jobId = inputJobId == null?"":inputJobId;
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String inputJobId) {
            jobId = inputJobId == null?"":inputJobId;
        }

        public String getJobStatus() {
            return jobStatus;
        }

        public void setJobStatus(String inputJobStatus) {
            jobStatus = inputJobStatus == null?"":inputJobStatus;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String inputCode) {
            code = inputCode == null?"":inputCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String inputMessage) {
            message = inputMessage == null?"":inputMessage;
        }
    }
}
