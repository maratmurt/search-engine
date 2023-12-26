package searchengine.dto;

import java.util.HashMap;
import java.util.Map;

public class ResponseObj {
    Map<String, Object> response = new HashMap<>();

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResult(boolean result) {
        response.put("result", result);
    }

    public void setError(String errorMessage) {
        response.put("error", errorMessage);
    }

    public void setData(String dataName, Object dataObj) {
        response.put(dataName, dataObj);
    }
}
