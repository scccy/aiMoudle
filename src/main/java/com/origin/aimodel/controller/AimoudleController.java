package com.origin.aimodel.controller;

import com.origin.aimodel.base.ResultData;
import com.origin.aimodel.dao.service.AiModelMpService;
import com.origin.aimodel.domain.mp.AiModelMp;
import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.domain.vo.AiTaskResult;

import com.origin.aimodel.util.spel.SpelDemo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AimoudleController {

    @Autowired
    AiModelMpService aiModelMpServiceImpl;

    @Autowired
    SpelDemo spelDemo;

    @PostMapping("/add")
    public ResultData<?> add(@RequestBody AiModelMp aiModelMp){
        try {
            aiModelMpServiceImpl.saveOrUpdate(aiModelMp);
        }
        catch (Exception e){
            return ResultData.fail(e.getMessage());
        }
        return ResultData.ok();
    }


//   @PostMapping("/test/taskStart")
//    public ResultData<?> testTaskStart(@RequestBody AiTaskQuery aiTaskQuery){
//
//       try {
//           AiTaskResult aiTaskResult = spelDemo.taskStart(aiTaskQuery);
//       }
//       catch (Exception e){
//           return ResultData.fail(e.getMessage());
//       }
//       return ResultData.ok(aiTaskQuery);
//   }


}
