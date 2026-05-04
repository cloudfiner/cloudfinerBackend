package com.aws.serviceImpl;
import com.aws.entity.NotificationTemplate;
import com.aws.repository.NotificationTemplateRepository;
import com.aws.service.TemplateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemplateServiceImpl  implements TemplateService{

    @Autowired
    private NotificationTemplateRepository repository;

    public String buildMessage(String key, Object... args)  {

        NotificationTemplate template = repository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Template not found: " + key));

        String msg = template.getTemplate();

        // simple replace {0}, {1}
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return msg;
    }

    public String getPriority(String key) {
        return repository.findByKey(key)
                .map(NotificationTemplate::getDefaultPriority)
                .orElse("INFO");
    }
}