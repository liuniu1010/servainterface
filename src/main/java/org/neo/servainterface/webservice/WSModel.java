package org.neo.servainterface.webservice;

public class WSModel {
    public static class AIChatParams {
        String userInput;
        String fileContent;

        public String getUserInput() {
            return userInput;
        }

        public void setUserInput(String inputUserInput) {
            userInput = inputUserInput;
        }

        public String getFileContent() {
            return fileContent;
        }

        public void setFileContent(String inputFileContent) {
            fileContent = inputFileContent;
        }
    }

    public static class AIChatResponse {
        private boolean isSuccess;
        private String message;

        public AIChatResponse(boolean inputIsSuccess, String inputMessage) {
            isSuccess = inputIsSuccess;
            message = inputMessage;
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
            message = inputMessage;
        }
    }
}
