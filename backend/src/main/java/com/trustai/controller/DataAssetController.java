package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.DataAsset;
import com.trustai.service.DataAssetService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/data-asset")
public class DataAssetController {
    @Autowired private DataAssetService dataAssetService;

    @GetMapping("/list")
    public R<List<DataAsset>> list(@RequestParam(required = false) String name) {
        QueryWrapper<DataAsset> qw = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) qw.like("name", name);
        return R.ok(dataAssetService.list(qw));
    }

    @PostMapping("/register")
    public R<?> register(@RequestBody DataAsset asset) {
        dataAssetService.save(asset);
        return R.okMsg("注册成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody DataAsset asset) {
        dataAssetService.updateById(asset);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody IdReq req) {
        dataAssetService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { public Long getId(){return id;} public void setId(Long id){this.id=id;} private Long id; }
}
