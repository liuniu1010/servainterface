package org.neo.servainterface.webservice;

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
