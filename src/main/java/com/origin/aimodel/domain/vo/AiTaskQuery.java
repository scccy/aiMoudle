package com.origin.aimodel.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiTaskQuery {
    public String modelName;
    public Map<String,Object> params = new HashMap<>();
}
