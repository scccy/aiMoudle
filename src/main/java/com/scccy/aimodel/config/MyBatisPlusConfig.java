package com.scccy.aimodel.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Configuration
public class MyBatisPlusConfig {
    
    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
        System.out.println("============== 分页插件加载成功 ==============");
        return interceptor;
    }



    /**
     * 自动填充处理器
     */
    @Component
    public static class MetaObjectHandlerImpl implements MetaObjectHandler {

        
        @Override
        public void insertFill(MetaObject metaObject) {
            String currentUser = "test";
            // 插入时自动填充
            // 处理 LocalDateTime 类型的字段
            this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
            // 填充创建人和更新人
            this.strictInsertFill(metaObject, "createdBy", String.class, currentUser);
            this.strictInsertFill(metaObject, "updatedBy", String.class, currentUser);
            this.strictInsertFill(metaObject, "delFlag", Integer.class, 0);

        }
        
        @Override
        public void updateFill(MetaObject metaObject) {
            String currentUser = "test";
            this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
            this.strictUpdateFill(metaObject, "updatedBy", String.class, currentUser);
        }
    }
}

