package com.blog.service;

import com.blog.entity.OperationLog;
import com.blog.mapper.OperationLogMapper;
import com.blog.security.ClientIpResolver;
import com.blog.security.LoginUser;
import com.blog.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogService {

    private final OperationLogMapper logMapper;
    private final ClientIpResolver ipResolver;

    @Autowired
    public OperationLogService(OperationLogMapper logMapper, ClientIpResolver ipResolver) {
        this.logMapper = logMapper;
        this.ipResolver = ipResolver;
    }

    /** 记录一条操作日志 */
    public void record(String module, String action, String target, String detail) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setTarget(target == null ? "" : target);
        log.setDetail(detail == null ? "" : detail);
        LoginUser user = SecurityUtil.current();
        log.setOperator(user != null ? user.getUsername() : "system");
        log.setIp(currentIp());
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    /** 查询某模块的最近日志(倒序) */
    public List<OperationLog> recent(String module, int limit) {
        return logMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getModule, module)
                .orderByDesc(OperationLog::getCreateTime)
                .last("LIMIT " + limit)
        );
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest req = attrs != null ? attrs.getRequest() : null;
            return ipResolver.resolve(req);
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }
}
