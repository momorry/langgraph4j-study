package com.moli.common.dao;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.ConstVal;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.TemplateConfig;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.po.TableFill;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;

/**
 * @author moli
 */
public class MybatisGenerator {

    public static void main(String[] args) {
        boolean startWithI = true;
        generateByTables(startWithI);

        System.getProperty("user.dir");
        System.out.println(System.getProperty("user.dir"));

    }

    //生成指定表 包含和出去二选一,如果都不填，则全库的表
    private static String[] includeTables = new String[]{
            "mo_chat","mo_message_history"
    };

    private static String[] excludeTableNames = new String[]{};

    /**
     * 配置数据源
     *
     * @return 数据源配置 DataSourceConfig
     */

    private static DataSourceConfig getDataSourceConfig() {

        return new DataSourceConfig().setDbType(DbType.MYSQL)
                .setUrl("jdbc:mysql://xx.129.67.11:4000/xxx?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true&useCursorFetch=true&rewriteBatchedStatements=true")
                //用时再填
                .setUsername("xx")
                //用时再填
                .setPassword("xx@2024")
                .setDriverName("com.mysql.cj.jdbc.Driver")
                .setTypeConvert(new MySqlTypeConvert() {
                    @Override
                    public IColumnType processTypeConvert(GlobalConfig globalConfig, String fieldType) {
                        if (fieldType.contains("tinyint") || fieldType.contains("bit")) {
                            return DbColumnType.INTEGER;
                        }
                        if (fieldType.contains("double")) {
                            return DbColumnType.BIG_DECIMAL;
                        }

                        return super.processTypeConvert(globalConfig, fieldType);
                    }
                });
    }

    /**
     * 根据表自动生成
     *
     * @param serviceNameStartWithI 默认为false
     */
    private static void generateByTables(boolean serviceNameStartWithI) {
        //配置数据源
        DataSourceConfig dataSourceConfig = getDataSourceConfig();
        // 策略配置
        StrategyConfig strategyConfig = getStrategyConfig();
        //全局变量配置
        GlobalConfig globalConfig = getGlobalConfig(serviceNameStartWithI);
        //包名配置
        PackageConfig packageConfig = getPackageConfig();
        //模板配置
        TemplateConfig templateConfig = getTemplateConfig();
        //自动生成
        atuoGenerator(dataSourceConfig, strategyConfig, globalConfig, packageConfig, templateConfig);
    }

    /**
     * 集成
     *
     * @param dataSourceConfig 配置数据源
     * @param strategyConfig   策略配置
     * @param config           全局变量配置
     * @param packageConfig    包名配置
     * @param templateConfig
     */

    private static void atuoGenerator(DataSourceConfig dataSourceConfig, StrategyConfig strategyConfig, GlobalConfig config, PackageConfig packageConfig, TemplateConfig templateConfig) {
        new AutoGenerator()
                .setGlobalConfig(config)
                .setDataSource(dataSourceConfig)
                .setStrategy(strategyConfig)
                .setPackageInfo(packageConfig)
                .setTemplateEngine(new VelocityTemplateEngine())
                .setTemplate(templateConfig)
                .execute();
    }

    /**
     * 设置包名
     *
     * @return PackageConfig 包名配置
     */

    private static PackageConfig getPackageConfig() {
        String basePath = "langgraph-api/src/main/java";
        String resBasePath = "langgraph-api/src/main";
        HashMap<String, String> pathInfo = new HashMap<>(10);
        pathInfo.put(ConstVal.XML_PATH, resBasePath + "/resources/mapper");
        pathInfo.put(ConstVal.MAPPER_PATH, basePath + "/com/moli/common/dao/mapper");
        pathInfo.put(ConstVal.SERVICE_PATH, basePath + "/com/moli/common/dao/manager");
        pathInfo.put(ConstVal.SERVICE_IMPL_PATH, basePath + "/com/moli/common/dao/manager/impl");
        pathInfo.put(ConstVal.ENTITY_PATH, basePath + "/com/moli/common/dao/entity");
//        pathInfo.put(ConstVal.CONTROLLER_PATH,basePath+"/com/moli/controller");
        return new PackageConfig()
                .setParent("com.moli.common.dao")
                .setXml("mapper")
                .setMapper("mapper")
                .setService("manager")
                .setServiceImpl("manager.impl")
                .setEntity("entity")
//                .setController("controller")
                .setPathInfo(pathInfo);
    }

    /**
     * 全局配置
     *
     * @param serviceNameStartWithI false
     * @return GlobalConfig
     */

    private static GlobalConfig getGlobalConfig(boolean serviceNameStartWithI) {
        GlobalConfig globalConfig = new GlobalConfig();
        //获取当前项目绝对路径
        String absPath = System.getProperty("user.dir");
        globalConfig
                .setBaseColumnList(true)
                .setBaseResultMap(true)
                .setActiveRecord(false)
                .setAuthor("system")
                //设置输出路径
                .setOutputDir(absPath)
                .setServiceName("%sManager")
                .setServiceImplName("%sManagerImpl")
                .setFileOverride(true);
        if (!serviceNameStartWithI) {
            //设置service名
            globalConfig.setServiceName("%sManager");
        }
        return globalConfig;
    }

    /**
     * 策略配置
     *
     * @return StrategyConfig
     */

    private static StrategyConfig getStrategyConfig() {
        List<TableFill> tableFills = Lists.newArrayList();
        TableFill tableFill1 = new TableFill("deleted", FieldFill.INSERT);
        TableFill tableFill2 = new TableFill("create_by", FieldFill.INSERT);
        TableFill tableFill3 = new TableFill("create_time", FieldFill.INSERT);
        TableFill tableFill4 = new TableFill("update_by", FieldFill.INSERT_UPDATE);
        TableFill tableFill5 = new TableFill("update_time", FieldFill.INSERT_UPDATE);

        tableFills.add(tableFill1);
        tableFills.add(tableFill2);
        tableFills.add(tableFill3);
        tableFills.add(tableFill4);
        tableFills.add(tableFill5);

        StrategyConfig strategyConfig = new StrategyConfig()
                // 全局大写命名 ORACLE 注意
                .setCapitalMode(true)
                .setEntityLombokModel(true)
                .setRestControllerStyle(false)
                .setTableFillList(tableFills)
                //从数据库表到文件的命名策略
                .setNaming(NamingStrategy.underline_to_camel)
                .setColumnNaming(NamingStrategy.underline_to_camel);
        //需要生成的的表名，多个表名传数组
        if (includeTables.length > 0) {
            strategyConfig.setInclude(includeTables);
        } else {
            strategyConfig.setExclude(excludeTableNames);
        }
        return strategyConfig;
    }


    private static TemplateConfig getTemplateConfig() {
        TemplateConfig templateConfig = new TemplateConfig();
        templateConfig.setMapper("templates/mapper.java.vm");
        return templateConfig;
    }


}