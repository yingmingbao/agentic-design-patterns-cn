package com.strategist.ai.advice;

import com.alibaba.fastjson2.JSONObject;
import com.strategist.ai.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.MessageFormat;
import java.util.List;

@ControllerAdvice
@Slf4j
public class ControllerErrorAdvice {

    @ExceptionHandler
    @ResponseBody
    public String handleException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        if (bindingResult.hasErrors()) {

            List<ObjectError> allErrors = bindingResult.getAllErrors();
            if (allErrors != null && allErrors.size() > 0) {
                Result result = Result.error(500, allErrors.get(0).getDefaultMessage());
                // ResponseBodyVO responseBody =  ResponseBodyVO.response(500, allErrors.get(0).getDefaultMessage()).build();
                String jsonString = JSONObject.toJSONString(result);
                log.error(MessageFormat.format("response", jsonString));
                return jsonString;
            }
        }
        return JSONObject.toJSONString(Result.error(500, "调用接口异常"));

    }

}
