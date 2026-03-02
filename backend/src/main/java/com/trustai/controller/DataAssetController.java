package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.DataAsset;
import com.trustai.service.DataAssetService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/data-asset")
@Validated
public class DataAssetController {
    @Autowired private DataAssetService dataAssetService;

    @GetMapping("/list")
    public R<List<DataAsset>> list(@RequestParam(required = false) String name) {
        QueryWrapper<DataAsset> qw = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) qw.like("name", name);
        return R.ok(dataAssetService.list(qw));
    }

    @PostMapping("/register")
    public R<?> register(@RequestBody @Validated DataAssetReq asset) {
        DataAsset entity = new DataAsset();
        entity.setName(asset.getName());
        entity.setType(asset.getType());
        entity.setSensitivityLevel(asset.getSensitivityLevel());
        entity.setOwnerId(asset.getOwnerId());
        entity.setLocation(asset.getLocation());
        dataAssetService.save(entity);
        return R.okMsg("注册成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody @Validated DataAssetUpdateReq asset) {
        DataAsset entity = new DataAsset();
        entity.setId(asset.getId());
        entity.setName(asset.getName());
        entity.setType(asset.getType());
        entity.setSensitivityLevel(asset.getSensitivityLevel());
        entity.setOwnerId(asset.getOwnerId());
        entity.setLocation(asset.getLocation());
        dataAssetService.updateById(entity);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        dataAssetService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { @NotNull private Long id; public Long getId(){return id;} public void setId(Long id){this.id=id;} }
    public static class DataAssetReq {
        @NotBlank private String name;
        @NotBlank private String type;
        private String sensitivityLevel;
        @NotNull private Long ownerId;
        private String location;
        public String getName(){return name;} public void setName(String v){name=v;}
        public String getType(){return type;} public void setType(String v){type=v;}
        public String getSensitivityLevel(){return sensitivityLevel;} public void setSensitivityLevel(String v){sensitivityLevel=v;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public String getLocation(){return location;} public void setLocation(String v){location=v;}
    }
    public static class DataAssetUpdateReq extends DataAssetReq { @NotNull private Long id; public Long getId(){return id;} public void setId(Long v){id=v;} }
}
