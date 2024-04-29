package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorResponse extends ApiResponse {
    private String error;

    public ErrorResponse(String error) {
        super.setResult(false);
        this.error = error;
    }
}
