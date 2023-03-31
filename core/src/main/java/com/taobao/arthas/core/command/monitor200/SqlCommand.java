package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.*;

import java.lang.instrument.Instrumentation;
import java.util.List;

@Name("sql")
@Summary("run a query sql and out put json result!")
@Description(Constants.EXAMPLE
        + "  sql \"select * from t_device where machine_sn = 'abc' limit 1;\"  -f '/app/file.json''\n"
        + "  sql \"update t_device set create_time='2023-01-01 22:22:22' where id=957\" -u'\n"
        + "  sql \"delete from  t_device where id=1100\" -e\n"
        + Constants.WIKI + Constants.WIKI_HOME + "invoke")
//@formatter:on
public class SqlCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(SqlCommand.class);

    private String sql;

    private String classLoaderClass;

    private String fileName;

    private Integer instanceIndex;

    private Boolean isExecute;

    private Boolean isUpdate;

    @Argument(index = 0, argName = "sql", required = true)
    @Description("想要执行的sql")
    public void setExpress(String sql) {
        this.sql = sql;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(longName = "fileName",shortName = "f")
    @Description("json输出的目标文件（不会自动创建文件夹）")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Option(longName = "instanceIndex",shortName = "i")
    @DefaultValue("0")
    @Description("兼容多datasource场景，手动指定第几个datasource，后续应该可以使用名字指定")
    public void setInstanceIndex(Integer instanceIndex) {
        this.instanceIndex = instanceIndex;
    }

    @Option(shortName = "e", longName = "execute", flag = true)
    @Description("使用execute执行sql")
    public void setExecute(boolean execute) {
        this.isExecute = execute;
    }

    @Option(shortName = "u", longName = "update", flag = true)
    @Description("使用update执行sql")
    public void setUpdate(boolean update) {
        this.isUpdate = update;
    }


    @Override
    public void process(final CommandProcess process) {
        //目前好像都是每次使用时创建，没有保存，不知道为啥
        VmToolCommand vmToolCommand = new VmToolCommand();
        vmToolCommand.setAction(VmToolCommand.VmToolAction.getInstances);
        vmToolCommand.setClassName("org.springframework.jdbc.core.JdbcTemplate");
        if (StringUtils.isBlank(classLoaderClass)){
            String defaultClassLoaderName = "org.springframework.boot.loader.LaunchedURLClassLoader";
            Instrumentation inst = process.session().getInstrumentation();
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, defaultClassLoaderName);
            if (matchedClassLoaders == null || !matchedClassLoaders.isEmpty()){
                vmToolCommand.setClassLoaderClass(defaultClassLoaderName);
            }
        } else {
            vmToolCommand.setClassLoaderClass(this.classLoaderClass);
        }

        String transSql = sql.replace("'","\\\"");

        String newExpress = "";
        if (isUpdate){
            newExpress =
                    "@java.lang.Thread@currentThread().setContextClassLoader(instances["+instanceIndex+"].class.getClassLoader()),"
                            +"#list=instances["+instanceIndex+"].queryForList(\"explain "+transSql+"\"),"
                            + "#rows=#list[0][\"rows\"].longValue(),"
                            + "#rows > 100 ? \"effect rows is gt 100! do not allow to update!\" : instances["+instanceIndex+"].update(\""+transSql+"\")"
            ;
        }else if (isExecute){
            newExpress =
                    "@java.lang.Thread@currentThread().setContextClassLoader(instances["+instanceIndex+"].class.getClassLoader()),"
                            +"#list=instances["+instanceIndex+"].queryForList(\"explain "+transSql+"\"),"
                            + "#rows=#list[0][\"rows\"].longValue(),"
                            + "#rows > 100 ? \"effect rows is gt 100! do not allow to execute!\" : instances["+instanceIndex+"].execute(\""+transSql+"\")"
            ;
        }else {
            newExpress =
                    "@java.lang.Thread@currentThread().setContextClassLoader(instances["+instanceIndex+"].class.getClassLoader()),"
                            +"#list=instances["+instanceIndex+"].queryForList(\""+transSql+"\"),"
                            + "@com.alibaba.fastjson.JSON@toJSONString(#list)";

            if (!StringUtils.isBlank(fileName)){
                newExpress =
                        "@java.lang.Thread@currentThread().setContextClassLoader(instances["+instanceIndex+"].class.getClassLoader()),"
                                +"#list=instances["+instanceIndex+"].queryForList(\""+transSql+"\"),"
                                + "#content=@com.alibaba.fastjson.JSON@toJSONString(#list),"
                                + "#file=new java.io.File(\""+fileName+"\"),"
                                + "#fileOutputStream=new java.io.FileOutputStream(#file,false),"
                                + "#fileOutputStream.write(#content.getBytes()),"
                                + "#fileOutputStream.close(),"
                                + "\"result already write in " + fileName + "\""
                ;

            }
        }

        vmToolCommand.setExpress(newExpress);
        vmToolCommand.setExpand(1);
        vmToolCommand.setLimit(1);
        vmToolCommand.process(process);
    }

}
