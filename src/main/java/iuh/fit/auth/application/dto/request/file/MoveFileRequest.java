package iuh.fit.auth.application.dto.request.file;

import java.util.List;

public record MoveFileRequest(
        List<FileMoveRequest> requests,
        String correlationId
) {}
