package com.strategist.ai.service.component;

import com.volcengine.ark.runtime.model.responses.content.InputContentItemImage;
import com.volcengine.ark.runtime.model.responses.content.InputContentItemText;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.service.ArkService;
import com.volcengine.ark.runtime.model.responses.request.*;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Data
public class DoubaoAiConfigComponent {

    @Value("${volcengine.doubao.apikey}")
    private String apiKey;

    public static void main(String[] args) {
        String apiKey = "575ffaf6-f885-4f91-997e-55226426ead2";
        // 创建ArkService实例
        ArkService arkService = ArkService.builder().apiKey(apiKey).baseUrl("https://ark.cn-beijing.volces.com/api/v3").build();
        CreateResponsesRequest request = CreateResponsesRequest.builder()
                .model("doubao-seed-1-8-251228")
                .input(ResponsesInput.builder().addListItem(
                        ItemEasyMessage.builder().role(ResponsesConstants.MESSAGE_ROLE_USER).content(
                                MessageContent.builder()
                                        .addListItem(InputContentItemImage.builder().imageUrl("https://ark-project.tos-cn-beijing.volces.com/doc_image/ark_demo_img_1.png").build())
                                        .addListItem(InputContentItemText.builder().text("你看见了什么？").build())
                                        .build()
                        ).build()
                ).build())
                .build();
        ResponseObject resp = arkService.createResponse(request);
        System.out.println(resp);
        arkService.shutdownExecutor();
    }



}
