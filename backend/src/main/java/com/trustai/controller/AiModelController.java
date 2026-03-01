package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.AiModel;
import com.trustai.service.AiModelService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ai-model")
public class AiModelController {
    @Autowired private AiModelService aiModelService;

    @GetMapping("/list")
    public R<List<AiModel>> list(@RequestParam(required = false) String name) {
        QueryWrapper<AiModel> qw = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) qw.like("name", name);
        return R.ok(aiModelService.list(qw));
    }

    @PostMapping("/add")
    public R<?> add(@RequestBody AiModel model) {
        aiModelService.save(model);
        return R.okMsg("添加成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody AiModel model) {
        aiModelService.updateById(model);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody IdReq req) {
        aiModelService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { public Long getId(){return id;} public void setId(Long id){this.id=id;} private Long id; }
}
