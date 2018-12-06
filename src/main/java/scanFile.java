import org.apache.commons.io.FileUtils;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import jcifs.smb.SmbFile;
import jcifs.smb.NtlmPasswordAuthentication;


public class scanFile {

    @Resource
    private List<String> allUseless = new ArrayList<>();

    public List<String> addUseless(){
        String[] all = {"C:","D:","New folder","project","Local","Users","Program Files","AppData","A_Personal","desktop","work","xu","Desktop","desktop1","Siemens","$RECYCLE.BIN","Documents","Document","docxall","refer","task","Reference","Data","OTHER","Windows","Conference","pdf","pandas","images","tests","My Ebook","Drawings","Program Files (x86)","Customer","anaconda3","Excel VBA","reference","Microsoft","temp","text","AISC","Temp","$Recycle.Bin"};
        for (String a:all){
            allUseless.add(a);
        }
        return allUseless;
    }

    /**
     * 此函数用来从库中进行分词
     * 输入abbrdictc.txt
     * 输出对应的map
     * 此map 建立缩略词与所有的词库的关系
     * */
    public Map readTxt(String pathname) throws IOException {
        /*
         * 读入文件
         * */
//        File filename = new File(pathname);

        InputStream stream = getClass().getClassLoader().getResourceAsStream(pathname);
        File targetFile = new File(pathname);
        FileUtils.copyInputStreamToFile(stream, targetFile);

        InputStreamReader reader = new InputStreamReader(new FileInputStream(targetFile));
        BufferedReader br = new BufferedReader(reader);


        Map<String, List<String>> testMap = new HashMap<String, List<String>>();  //这是一个大的map
        /*
         * 逐行建立hashmap
         * */

        String line = "";
        line = br.readLine();
        int count=1;
        while (line != null) {

            List<String> aab=new ArrayList<>();     //用来保存value数组
            count++;
            String[] words=line.split("\\$");          //按照\$分词
            for (int i=0;i<words.length-1;i++){
                words[i]=words[i].substring(0,words[i].length()-1);
            }
            List list = Arrays.asList(words);
            Set set = new HashSet(list);                             //去重
            String [] rid=(String [])set.toArray(new String[0]);     //rid数组保存最后的结果数组
            String key=rid[0];                                         //获取key value
            for (int i=1;i<rid.length;i++){
                aab.add(rid[i]);
            }
            testMap.put(key,aab);                                        //放到大的map中去
            line = br.readLine(); // 一次读入一行数据
        }
        return testMap;
    }

    public void scanLog(String scanLog) throws IOException{
//        long timeStamp = System.currentTimeMillis();  //获取当前时间戳,也可以是你自已给的一个随机的或是别人给你的时间戳(一定是long型的数据)
//        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd-HH:mm");//这个是你要转成后的时间的格式
//        String sd = sdf.format(new Date(timeStamp));   // 时间戳转换成时间
        String path = "scan-progress.txt";
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path,true)));
        out.write(scanLog+"\r\n");
        out.close();
        //下面的过程用于记录扫描的过程
    }

    /**
     * 此函数与上个函数做区分
     * 从扫描后的List中进行操作
     * */
    public void ListClassific(ArrayList<String> scanFiles)throws IOException{
        int count=0;
        String scanLog="";
        for (String line:scanFiles){
            count++;
            if (count==1){
                scanLog=String.format("scan File started");
                scanLog(scanLog);
            }
            if(count == scanFiles.size()){
                scanLog=String.format("scan %d File ended",count);
                scanLog(scanLog);
            }
            if(count%100==0){
                scanLog=String.format("it scans to %d file and the path is %s",count,line);
                scanLog(scanLog);
            }
            System.out.println("count:"+count);
            classific(line);
        }

    }

    /**
     * 此函数用于进行分类
     * 输入单条路径path
     * */
    public void classific(String path) throws IOException{

        //对路径进行分词 按照/来分
        String[] words = path.split("\\\\");   //windows
//        String[] words = path.split("/");      //Mac

        String lastExcept =words[words.length-1].split("\\.")[0];
        words[words.length-1] = lastExcept;

        //多个list来保存路径中的有用信息
        //allUseless为所有的无效单词
        //inEntity为在总的实体库中的单词
        //inUseless为该路径中无效的单词
        //regex为正则表达式匹配出来的单词
        List<String> allUseless = addUseless();
        Set<String> inEntity = new HashSet<>();
        Set<String> inUseless = new HashSet<>();
        Set<String> regex = new HashSet<>();
//        String allPath = "/Users/hufangzhou/Desktop/abbrdictc.txt";
        String allPath = "abbrdictc.txt";
        for (String word:words){
            //先在无效单词库中查找
            if (allUseless.contains(word)){
                inUseless.add(word);
            }
            else {
                ///在实体库中查找
                if (existInEntity(word,allPath)) {
                    inEntity.add(word);
                }
                //大单词分割成小单词
                if (word.contains("_")){
                    String[] strings = word.split("_");
                    for (String s:strings){
                        if (existInEntity(s,allPath)) {
                            inEntity.add(s);
                        }
                    }
                }
                //用正则表达式匹配的结果
                if (ruleAnalysis(word)){
                    regex.add(word);
                }
            }
        }
        //细化实体库中的实体
        Set<String> engineSet = NEGoutput("engine_dict.txt",inEntity,"engine");
        Set<String> componentSet =  NEGoutput("component_dict.txt",inEntity,"component");
        Set<String> analysisSet = NEGoutput("analysis_term_dict.txt",inEntity,"analysis");
        Set<String> designSet = NEGoutput("design_term_dict.txt",inEntity,"design");
        List<String> engineList = new ArrayList(engineSet);
        List<String> compontList = new ArrayList(componentSet);
        List<String> analysisList = new ArrayList(analysisSet);
        List<String> designList = new ArrayList(designSet);
        compontList.remove("");
        System.out.println(compontList.size());
        for (String s:compontList){
            System.out.println(s);
        }

        //如果是分析报告
        if(inEntity.size()>0&&engineSet.size()>0&&componentSet.size()>0&&analysisSet.size()>0&&designSet.size()==0){
            System.out.println("path :"+path);
            String engine = engineList.get(0);
            String componet = compontList.get(0);
            System.out.println("com: "+componet);
            String analysis = analysisList.get(0);
            System.out.println(String.format("entity->%s component->%s analysis->%s",engine,componet,analysis));
            String analysisPath = CreateAnalysisPath(engine,componet,analysis);
            copyFile(path,analysisPath);
            System.out.println();
        }

        //如果是设计报告
        else if(inEntity.size()>0&&engineSet.size()>0&&componentSet.size()>0&&analysisSet.size()==0&&designSet.size()>0){
            System.out.println("path :"+path);
            String engine = engineList.get(0);
            String componet = compontList.get(0);
            System.out.println("com: "+componet);
            String design = designList.get(0);
            System.out.println(String.format("entity->%s component->%s design->%s",engine,componet,design));
            String designPath = CreateDesignPath(engine,componet,design);
            copyFile(path,designPath);
        }
        System.out.println();
    }

    /**
     * 此函数用于创建目录
     * */
    public String CreateAnalysisPath(String engine,String component,String analysis) throws IOException{

//        String enginepath = readProperties("properties.json","serverPath");
        String enginepath = getProperties("serverPath");

        //创建engine目录
        enginepath = enginepath + "/" + engine;
        File file = new File(enginepath);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("engine Directory is created!");
            } else {
                System.out.println("Failed to create engine directory!");
            }
        }
        else {
            System.out.println("engine Directory already exists!");
        }

        //创建engine下的component目录
        String compentpath = enginepath + "/" + component;
        File comfile = new File(compentpath);
        if (!comfile.exists()) {
            if (comfile.mkdir()) {
                System.out.println("Component Directory is created!");
            } else {
                System.out.println("Failed to create Component directory!");
            }
        }
        else {
            System.out.println("component Directory already exists!");
        }



        //创建component下的analysis目录
        String analysisPath = compentpath + "/analysis";
        File analysisFile = new File(analysisPath);
        if (!analysisFile.exists()) {
            if (analysisFile.mkdir()) {
                System.out.println("Analysis Directory is created!");
            } else {
                System.out.println("Failed to create Analysis directory!");
            }
        }
        else {
            System.out.println("Analysis Directory already exists!");
        }


        //创建analysis下的目录
        String analysisLevelPath = analysisPath + "/" +analysis;
        File analysisLevelFile = new File(analysisLevelPath);
        if (!analysisLevelFile.exists()) {
            if (analysisLevelFile.mkdir()) {
                System.out.println("analysislevel Directory is created!");
            } else {
                System.out.println("Failed to create analysislevel directory!");
            }
        }
        else {
            System.out.println("analysislevel Directory already exists!");
        }
        return analysisLevelPath;
    }

    /**
     * 此函数用于创建目录
     * */
    public String CreateDesignPath(String engine,String component,String design) throws IOException{

//        String enginepath = readProperties("properties.json","serverPath");
        String enginepath = getProperties("serverPath");

        //创建engine目录
        enginepath = enginepath + "/" + engine;
        File file = new File(enginepath);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("engine Directory is created!");
            } else {
                System.out.println("Failed to create engine directory!");
            }
        }
        else {
            System.out.println("engine Directory already exists!");
        }

        //创建engine下的component目录
        String compentpath = enginepath + "/" + component;
        File comfile = new File(compentpath);
        if (!comfile.exists()) {
            if (comfile.mkdir()) {
                System.out.println("Component Directory is created!");
            } else {
                System.out.println("Failed to create Component directory!");
            }
        }
        else {
            System.out.println("component Directory already exists!");
        }

        //创建component下的design目录
        String designPath = compentpath + "/design";
        File designFile = new File(designPath);
        if (!designFile.exists()) {
            if (designFile.mkdir()) {
                System.out.println("Design Directory is created!");
            } else {
                System.out.println("Failed to create Design directory!");
            }
        }
        else {
            System.out.println("Design Directory already exists!");
        }

        //创建design下的目录
        String designLevelPath = designPath + "/" + design;
        File designLevelFile = new File(designLevelPath);
        if (!designLevelFile.exists()) {
            if (designLevelFile.mkdir()) {
                System.out.println("DesignLevel Directory is created!");
            } else {
                System.out.println("Failed to create DesignLevel directory!");
            }
        }
        else {
            System.out.println("DesignLevel Directory already exists!");
        }
        return designLevelPath;
    }

    /**
     * 此函数用于上传文件
     * */
    public void copyFile(String inPath,String outPath){
        int bytesum = 0;
        int byteread = 0;
        File oldfile = new File(inPath);

        //下面的代码片段用于加上时间戳
        String oldName = oldfile.getName();
        long timeStamp = System.currentTimeMillis();  //获取当前时间戳,也可以是你自已给的一个随机的或是别人给你的时间戳(一定是long型的数据)
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd-HH:mm");//这个是你要转成后的时间的格式
        String sd = sdf.format(new Date(timeStamp));   // 时间戳转换成时间
        System.out.println(sd);
        oldName = oldName + "_" + sd;

        try {
            if (oldfile.exists())
            { //文件存在时
                FileInputStream inStream = new FileInputStream(oldfile); //读入原文件
                FileOutputStream fs = new FileOutputStream(new File(outPath,oldName));
                byte[] buffer = new byte[5120];
                while ( (byteread = inStream.read(buffer)) != -1)
                {
                    bytesum += byteread; //字节数 文件大小
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                uploadLog(inPath);
            }
        }
        catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
        }
    }

    /**
     * 此函数用于将上传文件的记录
     * */
    public void uploadLog(String Fpath) throws IOException {
        String line = "";
        long timeStamp = System.currentTimeMillis();  //获取当前时间戳,也可以是你自已给的一个随机的或是别人给你的时间戳(一定是long型的数据)
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd-HH:mm");//这个是你要转成后的时间的格式
        String sd = sdf.format(new Date(timeStamp));   // 时间戳转换成时间
        System.out.println(sd);
        line = line + Fpath  + " uploaded at " + sd;
        String path = "log.txt";
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path,true)));
        out.write(line+"\r\n");
        out.close();
    }


    /**
     * 此函数用于细化属于实体的单词
     * 判断每一步实体具体的层次
     * 输入为具体层次实体库的路径、所有在实体库中的实体，以及要检测的实体
     * */
    public Set NEGoutput(String pathname,Set<String> inEntity,String entity) throws IOException{
        Set<String> set = new HashSet<>();
        for (String s:inEntity){
            if (existInEntity(s,pathname)){
                set.add(s);
            }
        }
        return set;
    }


    /**
     * 此函数用于按照规则查找单词
     * */
    public boolean ruleAnalysis(String text){
        String pattern = "[0-9]{4}[A-Z]{1}";
        boolean isMatch = Pattern.matches(pattern,text);
        if (isMatch){
            return true;
        }
        else return false;

    }

    /***
     * 此函数判断某个单词是否在实体库中
     * 输入为某个单词 和实体库所在的文件路径
     */
    public boolean existInEntity(String entityTest,String pathname) throws IOException{

        //从路径中读取所有实体 返回一个Map 该map中key为缩略词 value为所有的形式
        Map<String, List<String>> testMap = new HashMap<String, List<String>>();  //这是一个大的map
        testMap=readTxt(pathname);
        boolean exist=false;
        //遍历实体库 如果key和value中包含该单词 返回true
        for (Map.Entry<String, List<String>> entry : testMap.entrySet())
        {
            if (entry.getValue().contains(entityTest)||entry.getKey().equals(entityTest)) {
//                System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
                exist=true;
            }
        }
        return exist;
    }

    public static String getProperties(String key){
        if (key == null || key == "" || "".equals(key)) {
            return null;
        }
        Properties properties = new Properties();
        File file = new File("config.properties");
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            properties.load(fis);
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties.getProperty(key);
    }

    public static void main(String[] args) throws Exception {
        scanFile scanFile = new scanFile();
        System.out.println("Hello world");
        //人为添加无效单词
//        scanFile.addUseless();

        scanFile.smb();

//        //读取配置json文件
//        String folderPath = getProperties("folderPath");
//        System.out.println("folderPath: "+folderPath);
//        scanFilesWithRecursion(folderPath);
        //开始扫描文件

//        scanFile.ListClassific(scanFiles);
    }

    public void smb(){
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("10.141.245.17","huhu","huhu");
        String url = "smb://10.141.245.17/ftp/Knight.zip";
        try {
            SmbFile remoteFile = new SmbFile(url, auth);
            if (remoteFile.exists()) {
                remoteFile.delete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void readShare() throws Exception{
        //smb://xxx:xxx@192.168.2.188/testIndex/
        //xxx:xxx是共享机器的用户名密码
        String url="smb://huhu:huhu@10.141.245.17/ftp/";
        SmbFile file = new SmbFile(url);
        if(file.exists()){
            SmbFile[] files = file.listFiles();
            for(SmbFile f : files){
                System.out.println(f.getName());
            }
        }
    }

    private static ArrayList<String> scanFiles = new ArrayList<String>();

    /**
     * 此函数递归扫描指定文件夹
     * 将其中的文件路径加入到scanFiles中
     * */
    public static ArrayList<String> scanFilesWithRecursion(String folderPath) {
        ArrayList<String> dirctorys = new ArrayList<String>();
        File directory = new File(folderPath);
        if (directory.isDirectory()) {
            File[] filelist = directory.listFiles();
            for (int i = 0; i < filelist.length; i++) {
                /**如果当前是文件夹，进入递归扫描文件夹**/
                if (filelist[i].isDirectory()) {
                    dirctorys.add(filelist[i].getAbsolutePath());
                    /**递归扫描下面的文件夹**/
                    scanFilesWithRecursion(filelist[i].getAbsolutePath());
                }
                /**非文件夹**/
                else {
                    scanFiles.add(filelist[i].getAbsolutePath());
                }
            }
        }
        return scanFiles;
    }
}
