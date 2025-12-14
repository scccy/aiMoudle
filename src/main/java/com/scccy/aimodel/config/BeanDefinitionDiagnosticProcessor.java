package com.scccy.aimodel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean定义诊断处理器
 * 用于诊断 Spring 6 中 factoryBeanObjectType 属性类型问题
 */
@org.springframework.context.annotation.Configuration
public class BeanDefinitionDiagnosticProcessor implements BeanFactoryPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(BeanDefinitionDiagnosticProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.info("============== 开始诊断 BeanDefinition ==============");

        List<String> fixedBeans = new ArrayList<>();
        List<String> unresolvedBeans = new ArrayList<>();
        List<String> invalidTypeBeans = new ArrayList<>();

        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);

            // Spring 6 期望该属性为 ResolvableType 或 Class<?>，否则会抛出 IllegalArgumentException
            Object factoryBeanObjectType = beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
            if (factoryBeanObjectType == null) {
                continue;
            }

            if (factoryBeanObjectType instanceof ResolvableType || factoryBeanObjectType instanceof Class<?>) {
                continue;
            }

            String source = beanDefinition.getResourceDescription();
            if (factoryBeanObjectType instanceof String typeName) {
                try {
                    Class<?> resolved = ClassUtils.forName(typeName, beanFactory.getBeanClassLoader());
                    beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ResolvableType.forClass(resolved));
                    fixedBeans.add(String.format("%s -> %s (source: %s)", beanName, resolved.getName(), source));
                } catch (ClassNotFoundException ex) {
                    unresolvedBeans.add(String.format("%s -> [%s] 无法解析为类 (source: %s)", beanName, typeName, source));
                }
            } else {
                invalidTypeBeans.add(String.format("%s -> %s (source: %s)", beanName, factoryBeanObjectType.getClass().getName(), source));
            }
        }

        if (!fixedBeans.isEmpty()) {
            log.warn("修复了 {} 个 factoryBeanObjectType 为 String 的 Bean，避免 Spring 解析失败。", fixedBeans.size());
            fixedBeans.forEach(msg -> log.warn("  {}", msg));
        }

        if (!unresolvedBeans.isEmpty() || !invalidTypeBeans.isEmpty()) {
            log.error("仍存在无法自动修复的 factoryBeanObjectType 属性：");
            unresolvedBeans.forEach(msg -> log.error("  {}", msg));
            invalidTypeBeans.forEach(msg -> log.error("  {}", msg));
        }

        if (fixedBeans.isEmpty() && unresolvedBeans.isEmpty() && invalidTypeBeans.isEmpty()) {
            log.info("✓ 未发现 factoryBeanObjectType 类型异常的 Bean");
        }

        log.info("============== 诊断完成 ==============");
    }

    @Override
    public int getOrder() {
        // 设置为最高优先级，确保在其他 BeanFactoryPostProcessor 之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
