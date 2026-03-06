package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.AiModel;
import com.trustai.service.AiModelService;
import com.trustai.utils.R;
import com.trustai.utils.AesEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ai-model")
@Validated
public class AiModelController {
    @Autowired private AiModelService aiModelService;
    @Autowired private AesEncryptor aesEncryptor;

    @GetMapping("/list")
    public R<List<AiModel>> list(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false, name = "name") String legacyName) {
        QueryWrapper<AiModel> qw = new QueryWrapper<>();
        String kw = StringUtils.hasText(keyword) ? keyword : legacyName;
        if (StringUtils.hasText(kw)) {
            qw.and(q -> q.like("model_name", kw).or().like("model_code", kw));
        }
        List<AiModel> models = aiModelService.list(qw);
        models.forEach(m -> m.setApiKey(aesEncryptor.mask(m.getApiKey())));
        return R.ok(models);
    }

    @PostMapping("/add")
    public R<?> add(@RequestBody @Validated ModelReq model) {
        boolean exists = aiModelService.count(new QueryWrapper<AiModel>().eq("model_code", model.getModelCode())) > 0;
        if (exists) return R.error(40000, "模型编码已存在");
        AiModel entity = new AiModel();
        entity.setModelName(model.getModelName());
        entity.setModelCode(model.getModelCode());
        entity.setProvider(model.getProvider());
        entity.setApiUrl(model.getApiUrl());
        entity.setApiKey(aesEncryptor.encrypt(model.getApiKey()));
        entity.setModelType(model.getModelType());
        entity.setRiskLevel(model.getRiskLevel());
        entity.setStatus(model.getStatus());
        entity.setCallLimit(model.getCallLimit() == null ? 0 : model.getCallLimit());
        entity.setCurrentCalls(0);
        entity.setDescription(model.getDescription());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        aiModelService.save(entity);
        return R.okMsg("添加成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody @Validated ModelUpdateReq model) {
        AiModel existing = aiModelService.getById(model.getId());
        if (existing == null) return R.error(40000, "模型不存在");
        boolean dup = aiModelService.count(new QueryWrapper<AiModel>().eq("model_code", model.getModelCode()).ne("id", model.getId())) > 0;
        if (dup) return R.error(40000, "模型编码已存在");
        existing.setModelName(model.getModelName());
        existing.setModelCode(model.getModelCode());
        existing.setProvider(model.getProvider());
        existing.setApiUrl(model.getApiUrl());
        if (StringUtils.hasText(model.getApiKey())) {
            existing.setApiKey(aesEncryptor.encrypt(model.getApiKey()));
        }
        existing.setModelType(model.getModelType());
        existing.setRiskLevel(model.getRiskLevel());
        existing.setStatus(model.getStatus());
        existing.setCallLimit(model.getCallLimit() == null ? existing.getCallLimit() : model.getCallLimit());
        existing.setDescription(model.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        aiModelService.updateById(existing);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        aiModelService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { @NotNull private Long id; public Long getId(){return id;} public void setId(Long id){this.id=id;} }
    public static class ModelReq {
        @NotBlank private String modelName;
        @NotBlank private String modelCode;
        @NotBlank private String provider;
        @NotBlank private String apiUrl;
        @NotBlank private String apiKey;
        @NotBlank private String modelType;
        private String riskLevel;
        private String status;
        private Integer callLimit;
        private String description;
        public String getModelName(){return modelName;} public void setModelName(String v){modelName=v;}
        public String getModelCode(){return modelCode;} public void setModelCode(String v){modelCode=v;}
        public String getProvider(){return provider;} public void setProvider(String v){provider=v;}
        public String getApiUrl(){return apiUrl;} public void setApiUrl(String v){apiUrl=v;}
        public String getApiKey(){return apiKey;} public void setApiKey(String v){apiKey=v;}
        public String getModelType(){return modelType;} public void setModelType(String v){modelType=v;}
        public String getRiskLevel(){return riskLevel;} public void setRiskLevel(String v){riskLevel=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;}
        public Integer getCallLimit(){return callLimit;} public void setCallLimit(Integer v){callLimit=v;}
        public String getDescription(){return description;} public void setDescription(String v){description=v;}
    }

    public static class ModelUpdateReq {
        @NotNull private Long id;
        @NotBlank private String modelName;
        @NotBlank private String modelCode;
        @NotBlank private String provider;
        @NotBlank private String apiUrl;
        private String apiKey; // optional on update; only set when provided
        @NotBlank private String modelType;
        private String riskLevel;
        private String status;
        private Integer callLimit;
        private String description;
        public Long getId(){return id;} public void setId(Long v){id=v;}
        public String getModelName(){return modelName;} public void setModelName(String v){modelName=v;}
        public String getModelCode(){return modelCode;} public void setModelCode(String v){modelCode=v;}
        public String getProvider(){return provider;} public void setProvider(String v){provider=v;}
        public String getApiUrl(){return apiUrl;} public void setApiUrl(String v){apiUrl=v;}
        public String getApiKey(){return apiKey;} public void setApiKey(String v){apiKey=v;}
        public String getModelType(){return modelType;} public void setModelType(String v){modelType=v;}
        public String getRiskLevel(){return riskLevel;} public void setRiskLevel(String v){riskLevel=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;}
        public Integer getCallLimit(){return callLimit;} public void setCallLimit(Integer v){callLimit=v;}
        public String getDescription(){return description;} public void setDescription(String v){description=v;}
    }
}
