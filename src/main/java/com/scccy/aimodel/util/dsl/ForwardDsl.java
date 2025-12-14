package com.scccy.aimodel.util.dsl;

import com.scccy.aimodel.domain.mp.DimAiModelItemMp;
import com.scccy.aimodel.domain.mp.DimAiModelMp;
import com.scccy.aimodel.domain.vo.ForwardGenerateRequest;
import com.scccy.aimodel.domain.vo.ForwardRequestResult;

import java.util.List;

/**
 * Forward 子包入口：封装生成流程，供业务调用。
 */
public final class ForwardDsl {

    private ForwardDsl() {
    }

    public static Builder build(ForwardGenerateRequest request,
                                DimAiModelMp model,
                                List<DimAiModelItemMp> items) {
        return new Builder(request, model, items);
    }

    public static final class Builder {
        private final ForwardGenerateRequest request;
        private final DimAiModelMp model;
        private final List<DimAiModelItemMp> items;

        private Builder(ForwardGenerateRequest request,
                        DimAiModelMp model,
                        List<DimAiModelItemMp> items) {
            this.request = request;
            this.model = model;
            this.items = items;
        }

        public ForwardRequestResult generate() {
            return ForwardBuilder.build(request, model, items);
        }

    }
}
