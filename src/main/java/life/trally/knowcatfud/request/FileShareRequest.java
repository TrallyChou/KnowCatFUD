package life.trally.knowcatfud.request;

import lombok.Data;

@Data
//@NoArgsConstructor   // Java会自动生成无参构造器
public class FileShareRequest {
    public static final int PRIVATE = 0;  // 私人分享 可设密码 不参与排行 不支持搜索
    public static final int PUBLIC_RANKING = 1;  // 公开分享 无需密码 参与排行 支持搜索
    public static final int PUBLIC_UNRANKING = 2;  // 公开分享 无需密码 不参与排行 支持搜索
    public static final int PUBLIC_UNRANKING_UNSEARCHABLE = 3;  // 公开分享 无需密码 不参与排行 不支持搜索

    private Integer type = PUBLIC_RANKING;  // 分享类型
    private String password;
    private Integer expire;
    private String title;
    private String introduction;

}
