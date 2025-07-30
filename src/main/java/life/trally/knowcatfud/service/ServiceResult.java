package life.trally.knowcatfud.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ServiceResult<V,T> {
    private V result;
    private T data;
}
