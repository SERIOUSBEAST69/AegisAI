package com.trustai.controller;

import com.trustai.document.AuditLogDocument;
import com.trustai.service.AuditLogService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Date;
import java.util.List;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/audit-log")
@Validated
public class AuditLogController {
    @Autowired private AuditLogService auditLogService;

    @GetMapping("/search")
    public R<List<AuditLogDocument>> search(@RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) String operation,
                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date from,
                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date to) {
        return R.ok(auditLogService.search(userId, operation, from, to));
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        auditLogService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq {
        @NotNull private Long id;
        public Long getId(){return id;}
        public void setId(Long id){this.id=id;}
    }
}
