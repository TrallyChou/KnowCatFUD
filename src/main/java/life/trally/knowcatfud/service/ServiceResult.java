package life.trally.knowcatfud.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ServiceResult<RESULT,DATA> {
    private RESULT result;
    private DATA data;
}
