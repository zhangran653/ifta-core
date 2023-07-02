# Taint analysis

## Features

- Based on soot-infoflow 
- Support taint analysis for :
  - a directory of project classes, sources and libs, doesn't matter folder structure
  - a single jar file
  - a single zip file
- Flexible configurations
- Cached iCFG and Reusable infoflow instance to improve performance
- Auto locate source files
- Entry points auto selector
- JSP classes supported
- JSP line number reconstruct
- Can be used in library or standalone tool
- Easy to use

## How to use
> check out test cases in AnalysisExecutorTest.
### Use as a library:
```
    // Project to be analysis. Can be directory path, .jar file or .zip file path.
    String project = "/home/ran/Documents/work/thusa2/ifpc-testcase/WebGoat-5.0";
    // jdk path for the project. can be omitted if configuration file contains it or "libPath" of config includes it.
    String jdk = "/home/ran/Documents/work/thusa2/ifpc-testcase/jdk/rt.jar";
    String result = "test_result.json";
    AnalysisExecutor analysisExecutor = AnalysisExecutor
            .newInstance()
            .withDefaultConfig()
            .setProject(project)
            .setJDK(jdk)
            // default is "SPARK"
            .setCallGraphAlgorithm("CHA")
            // Track source file and calculate line number of jsp. Default false.
            .trackSourceFile(true)
            // Path reconstruction time out.
            .setTimeout(180)
            // write detect result to file.
            .writeOutput(true)
            .setOutput(result)
            .analysis();
    
    // detect result.
    var ruleResult = analysisExecutor.getRuleResult();
    System.out.println(ruleResult);

```

```
// User defined config.
   String configPath = "config4.json";
   String result = "test_result.json";
   AnalysisExecutor analysisExecutor = AnalysisExecutor
            .newInstance()
            .withConfig(configPath)
            .setCallGraphAlgorithm("CHA")
            // Track source file and calculate line number of jsp. Default false.
            .trackSourceFile(true)
            .writeOutput(true)
            .setOutput(result)
            .analysis();

   // detect result.
   var ruleResult = analysisExecutor.getRuleResult();
   System.out.println(ruleResult);

```

### Use as a standalone tool:
```
usage: ta [-h] [-dc {true,false}] [-c CONFIG] [-p PROJECT] [-j JDK]
          [-t {true,false}] [-w {true,false}] [-o OUTPUT]
          [-cg {CHA,SPARK,VTA,RTA,GEOM}] [-to TIMEOUT]

Run taint analysis of given project.

named arguments:
  -h, --help             show this help message and exit
  -dc {true,false}, --defaultconfig {true,false}
                         Specify if use default config. (default: true)
  -c CONFIG, --config CONFIG
                         User defined config file path.
  -p PROJECT, --project PROJECT
                         Project to be analysis.  Can  be directory path, .
                         jar file or .zip file path.
  -j JDK, --jdk JDK      Jdk path  for  the  project.  can  be  omitted  if
                         configuration file  contains  it  or  "libPath" of
                         config includes it.
  -t {true,false}, --track {true,false}
                         Track source file  and  calculate  line  number of
                         jsp files. (default: false)
  -w {true,false}, --write {true,false}
                         Write detect result to file. (default: true)
  -o OUTPUT, --output OUTPUT
                         Out put file path.
  -cg {CHA,SPARK,VTA,RTA,GEOM}, --callgraph {CHA,SPARK,VTA,RTA,GEOM}
                         Call graph algorithm. (default: SPARK)
  -to TIMEOUT, --timeout TIMEOUT
                         Path reconstruction time out. (default: 180)
```
### a example:
```
java17 -jar ta.jar -dc true -p "/home/ran/Documents/work/thusa2/ifpc-testcase/WebGoat-5.0" -j "/home/ran/Documents/work/thusa2/ifpc-testcase/jdk/rt.jar" -t true -w true -o result.json -cg CHA -to 180

```
### Config example: defaultconfig_sample.json

```
{
  "epoints": [
  ],
  "rules": [
    
    {
      "name": "sql injection",
      "sources" : [
        "<java.net.URLConnection: java.io.InputStream getInputStream()>",
        ...
      ],
      "sinks" : [
        "<java.sql.Connection: java.sql.CallableStatement prepareCall(java.lang.String)>",
        ...
      ]
    }
  ],

  "excludes": [
    "com.alibaba.*",
    "org.springframework.*"
  ],
  "autoDetect": true,
  "project": "youprojectpath",
  "jdk": "rt.jar"
}

```
> Other configurations check 'Config.java'

### Result example:
```
[
  {
    "ruleName": "命令注入",
    "result": [
      {
        "sourceSig": "<javax.servlet.GenericServlet: java.lang.String getInitParameter(java.lang.String)>",
        "sinkSig": "<java.lang.Runtime: java.lang.Process exec(java.lang.String[])>",
        "path": [
          {
            "file": "JavaSource/org/owasp/webgoat/session/WebSession.java",
            "javaClass": "org.owasp.webgoat.session.WebSession",
            "function": "<org.owasp.webgoat.session.WebSession: void <init>(javax.servlet.http.HttpServlet,javax.servlet.ServletContext)>",
            "jimpleStmt": "$r21 = virtualinvoke r4.<javax.servlet.http.HttpServlet: java.lang.String getInitParameter(java.lang.String)>(\"DatabaseDriver\")",
            "line": 275
          },
         ...
          {
            "file": "JavaSource/org/owasp/webgoat/util/Exec.java",
            "javaClass": "org.owasp.webgoat.util.Exec",
            "function": "<org.owasp.webgoat.util.Exec: org.owasp.webgoat.util.ExecResults execSimple(java.lang.String[])>",
            "jimpleStmt": "$r1 = staticinvoke <org.owasp.webgoat.util.Exec: org.owasp.webgoat.util.ExecResults execOptions(java.lang.String[],java.lang.String,int,int,boolean)>(r0, \"\", 0, 0, 0)",
            "line": 455
          },
          {
            "file": "JavaSource/org/owasp/webgoat/util/Exec.java",
            "javaClass": "org.owasp.webgoat.util.Exec",
            "function": "<org.owasp.webgoat.util.Exec: org.owasp.webgoat.util.ExecResults execOptions(java.lang.String[],java.lang.String,int,int,boolean)>",
            "jimpleStmt": "$r8 = virtualinvoke $r7.<java.lang.Runtime: java.lang.Process exec(java.lang.String[])>(r3)",
            "line": 103
          }
        ],
        "pathStm": [
          "$r21 = virtualinvoke r4.<javax.servlet.http.HttpServlet: java.lang.String getInitParameter(java.lang.String)>(\"DatabaseDriver\")",
          "r0.<org.owasp.webgoat.session.WebSession: java.lang.String databaseDriver> = $r21",
          "return",
          "r7 = $r3",
          "return r7",
          ...
          "r110 = virtualinvoke r9.<org.owasp.webgoat.lessons.CommandInjection: java.lang.String exec(org.owasp.webgoat.session.WebSession,java.lang.String[])>(r1, $r11)",
          "$r6 = staticinvoke <org.owasp.webgoat.util.Exec: org.owasp.webgoat.util.ExecResults execSimple(java.lang.String[])>(r2)",
          "$r1 = staticinvoke <org.owasp.webgoat.util.Exec: org.owasp.webgoat.util.ExecResults execOptions(java.lang.String[],java.lang.String,int,int,boolean)>(r0, \"\", 0, 0, 0)",
          "$r8 = virtualinvoke $r7.<java.lang.Runtime: java.lang.Process exec(java.lang.String[])>(r3)"
        ]
      },
    "resultCount": 16
  },
  {
    "ruleName": "路径操作",
    "result": [
      {
        "sourceSig": "<javax.servlet.ServletRequest: java.lang.String getParameter(java.lang.String)>",
        "sinkSig": "<java.io.File: void <init>(java.lang.String)>",
        "path": [
          {
            "file": "jsp-demo/org/apache/jsp/jsp_005fcustom_005fscript_005ffor_005foracle_jsp.java",
            "jspFile": "jsp-demo/jsp_custom_script_for_oracle.jsp",
            "javaClass": "org.apache.jsp.jsp_005fcustom_005fscript_005ffor_005foracle_jsp",
            "function": "<org.apache.jsp.jsp_005fcustom_005fscript_005ffor_005foracle_jsp: void _jspService(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>",
            "jimpleStmt": "$r43 = interfaceinvoke r2.<javax.servlet.http.HttpServletRequest: java.lang.String getParameter(java.lang.String)>(\"z2\")",
            "line": 477,
            "jspLine": 474
          },
          {
            "file": "jsp-demo/org/apache/jsp/jsp_005fcustom_005fscript_005ffor_005foracle_jsp.java",
            "jspFile": "jsp-demo/jsp_custom_script_for_oracle.jsp",
            "javaClass": "org.apache.jsp.jsp_005fcustom_005fscript_005ffor_005foracle_jsp",
            "function": "<org.apache.jsp.jsp_005fcustom_005fscript_005ffor_005foracle_jsp: void _jspService(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>",
            "jimpleStmt": "$r44 = virtualinvoke $r42.<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>($r43)",
            "line": 477,
            "jspLine": 474
          }
        ],
        "pathStm": [
          "$r43 = interfaceinvoke r2.<javax.servlet.http.HttpServletRequest: java.lang.String getParameter(java.lang.String)>(\"z2\")",
          "$r44 = virtualinvoke $r42.<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>($r43)",
          "$r45 = virtualinvoke $r44.<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>(\"\")",
          "$r46 = virtualinvoke $r45.<java.lang.StringBuilder: java.lang.String toString()>()",
          "$r48 = virtualinvoke r0.<org.apache.jsp.jsp_005fcustom_005fscript_005ffor_005foracle_jsp: java.lang.String decode(java.lang.String,java.lang.String)>($r46, $r47)",
          "return r17",
          "$r49 = virtualinvoke r0.<org.apache.jsp.jsp_005fcustom_005fscript_005ffor_005foracle_jsp: java.lang.String EC(java.lang.String)>($r48)",
          "return r2",
          "$r58[2] = $r49",
          "$r98 = $r58[1]",
          "virtualinvoke r0.<org.apache.jsp.jsp_005fcustom_005fscript_005ffor_005foracle_jsp: java.lang.String RenameFileOrDirCode(java.lang.String,java.lang.String)>($r98, $r97)",
          "specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>(r1)",
          "specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>(r1)"
        ],
        "resultCount": 127
    }
  ]
  
```

### TODO:
- Optimize deprecated. For now keep them to pass tests.
- Filter selectors based on configuration.
- More annotation to be collected.
- Cache classFilePath and fullClassName.
- Make entry point selector configurable.
- Jspc compile jsp to java. Incompatible with java 17+. Anyway the jsp compiler should be in a standalone module for best.
- Support parse from .smap files. But is it necessary? Is there any chance that JSP classes won't contain "sourceDebug"?
- ...
