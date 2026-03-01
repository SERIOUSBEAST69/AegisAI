package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.RiskEvent;
import com.trustai.service.RiskEventService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/risk-event")
public class RiskEventController {
    @Autowired private RiskEventService riskEventService;

    @GetMapping("/list")
    public R<List<RiskEvent>> list(@RequestParam(required = false) String type) {
        QueryWrapper<RiskEvent> qw = new QueryWrapper<>();
        if (type != null && !type.isEmpty()) qw.like("type", type);
        return R.ok(riskEventService.list(qw));
    }

    @PostMapping("/add")
    public R<?> add(@RequestBody RiskEvent event) {
        riskEventService.save(event);
        return R.okMsg("添加成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody RiskEvent event) {
        riskEventService.updateById(event);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody IdReq req) {
        riskEventService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { public Long getId(){return id;} public void setId(Long id){this.id=id;} private Long id; }
}
