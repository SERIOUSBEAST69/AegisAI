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
import java.util.List;

@RestController
@RequestMapping("/api/ai-model")
@Validated
public class AiModelController {
    @Autowired private AiModelService aiModelService;
    @Autowired private AesEncryptor aesEncryptor;

    @GetMapping("/list")
    public R<List<AiModel>> list(@RequestParam(required = false) String keyword) {
        QueryWrapper<AiModel> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like("model_name", keyword).or().like("model_code", keyword);
        }
        List<AiModel> models = aiModelService.list(qw);
        models.forEach(m -> m.setApiKey(aesEncryptor.mask(m.getApiKey())));
        return R.ok(models);
    }

    @PostMapping("/add")
    public R<?> add(@RequestBody @Validated ModelReq model) {
        AiModel entity = new AiModel();
        entity.setModelName(model.getModelName());
        entity.setModelCode(model.getModelCode());
        entity.setProvider(model.getProvider());
        entity.setApiUrl(model.getApiUrl());
        entity.setApiKey(aesEncryptor.encrypt(model.getApiKey()));
        entity.setModelType(model.getModelType());
        entity.setRiskLevel(model.getRiskLevel());
        entity.setStatus(model.getStatus());
        entity.setCallLimit(model.getCallLimit());
        entity.setCurrentCalls(0);
        entity.setDescription(model.getDescription());
        aiModelService.save(entity);
        return R.okMsg("添加成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody @Validated ModelUpdateReq model) {
        AiModel entity = new AiModel();
        entity.setId(model.getId());
        entity.setModelName(model.getModelName());
        entity.setModelCode(model.getModelCode());
        entity.setProvider(model.getProvider());
        entity.setApiUrl(model.getApiUrl());
        if (StringUtils.hasText(model.getApiKey())) {
            entity.setApiKey(aesEncryptor.encrypt(model.getApiKey()));
        }
        entity.setModelType(model.getModelType());
        entity.setRiskLevel(model.getRiskLevel());
        entity.setStatus(model.getStatus());
        entity.setCallLimit(model.getCallLimit());
        entity.setDescription(model.getDescription());
        aiModelService.updateById(entity);
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
    public static class ModelUpdateReq extends ModelReq { @NotNull private Long id; public Long getId(){return id;} public void setId(Long v){id=v;} }
}
