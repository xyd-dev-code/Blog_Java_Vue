package com.blog.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章导入结果汇总
 */
public class ImportResult {
    /** 新建的篇数(数据库此前无该 slug 的文章) */
    private int created = 0;
    /** 更新的篇数(命中既有 slug,走 update 路径) */
    private int updated = 0;
    /** 解析或入库失败的篇数 */
    private int failed = 0;
    /** 成功的条目明细 */
    private List<Item> items = new ArrayList<>();
    /** 失败的文件名 + 错误原因 */
    private List<String> errors = new ArrayList<>();

    public int getCreated() { return created; }
    public void setCreated(int created) { this.created = created; }
    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public int getTotal() { return created + updated + failed; }

    public static class Item {
        private String filename;
        private String title;
        private Long articleId;
        private boolean updated;

        public Item() {}
        public Item(String filename, String title, Long articleId, boolean updated) {
            this.filename = filename;
            this.title = title;
            this.articleId = articleId;
            this.updated = updated;
        }
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Long getArticleId() { return articleId; }
        public void setArticleId(Long articleId) { this.articleId = articleId; }
        public boolean isUpdated() { return updated; }
        public void setUpdated(boolean updated) { this.updated = updated; }
    }
}
